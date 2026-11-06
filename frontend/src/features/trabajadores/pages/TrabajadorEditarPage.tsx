import { useEffect, useRef, useState } from 'react';
import {
  Alert,
  App,
  Avatar,
  Button,
  Card,
  DatePicker,
  Divider,
  Flex,
  Form,
  Input,
  Modal,
  Spin,
  Typography,
  Upload,
} from 'antd';
import {
  CameraOutlined,
  DeleteOutlined,
  DownloadOutlined,
  FilePdfOutlined,
  SyncOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router';
import { useQueryClient } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import {
  descargarDocumento,
  eliminarDocumento,
  eliminarFoto,
  obtenerFotoBlob,
  subirDocumento,
  subirFoto,
  useEditarTrabajador,
  useTrabajador,
  type TipoDocumento,
  type TrabajadorEditarRequest,
} from '../api/trabajadores';
import { getErrorMessage, toFormFieldErrors } from '../../../shared/lib/apiError';
import { EstadoTag } from '../../../shared/components/EstadoTag';
import { PATHS } from '../../../routes/paths';
import { palette } from '../../../app/theme';

/**
 * US-26: Modificar trabajador.
 * US-28: Cargar, actualizar, renovar y eliminar documentación y foto.
 *
 * Muestra el nombre completo del trabajador como título y permite:
 * - Modificar Nombre y Apellido (DNI es de solo lectura, RN-11).
 * - Visualizar, subir, reemplazar y eliminar foto con confirmación.
 * - Visualizar estado, descargar, renovar y eliminar póliza de ART y Seguro de Vida.
 */
export function TrabajadorEditarPage() {
  const navigate = useNavigate();
  const { message } = App.useApp();
  const { id } = useParams<{ id: string }>();
  const trabajadorId = Number(id);

  const queryClient = useQueryClient();
  const { data: trabajador, isLoading, isError, error } = useTrabajador(trabajadorId);
  const editar = useEditarTrabajador(trabajadorId);

  const [form] = Form.useForm();
  const [fotoUrl, setFotoUrl] = useState<string | null>(null);
  const fotoBlobRef = useRef<string | null>(null);
  const [cargandoFoto, setCargandoFoto] = useState(false);
  const [errorArchivos, setErrorArchivos] = useState<string | null>(null);
  const [descargandoDoc, setDescargandoDoc] = useState<TipoDocumento | null>(null);
  const [guardando, setGuardando] = useState(false);

  // Estados locales para borrador de cambios (no mutan la API hasta presionar "Guardar cambios")
  const [fotoNuevaFile, setFotoNuevaFile] = useState<File | null>(null);
  const [fotoNuevaPreview, setFotoNuevaPreview] = useState<string | null>(null);
  const [fotoEliminada, setFotoEliminada] = useState(false);

  const [artNuevo, setArtNuevo] = useState<{ archivo: File; fecha: Dayjs } | null>(null);
  const [artEliminada, setArtEliminada] = useState(false);

  const [seguroNuevo, setSeguroNuevo] = useState<{ archivo: File; fecha: Dayjs } | null>(null);
  const [seguroEliminada, setSeguroEliminada] = useState(false);

  // Modal para borrar foto (simétrico según wireframes)
  const [modalBorrarFotoVisible, setModalBorrarFotoVisible] = useState(false);

  // Modal para borrar documento (simétrico según wireframes)
  const [modalBorrarDocTipo, setModalBorrarDocTipo] = useState<TipoDocumento | null>(null);

  // Estados para modal de carga/renovación de documento
  const [modalDocVisible, setModalDocVisible] = useState(false);
  const [modalDocTipo, setModalDocTipo] = useState<TipoDocumento | null>(null);
  const [modalDocArchivo, setModalDocArchivo] = useState<File | null>(null);
  const [modalDocFecha, setModalDocFecha] = useState<Dayjs | null>(null);
  const [modalDocError, setModalDocError] = useState<string | null>(null);

  // Cargar datos en el formulario cuando el trabajador esté disponible
  useEffect(() => {
    if (trabajador) {
      form.setFieldsValue({
        nombre: trabajador.nombre,
        apellido: trabajador.apellido,
      });
    }
  }, [trabajador, form]);

  // Cargar foto autenticada con JWT mediante Blob si el trabajador tiene foto (M-3 fix)
  useEffect(() => {
    let active = true;

    if (trabajador?.tieneFoto) {
      setCargandoFoto(true);
      obtenerFotoBlob(trabajadorId)
        .then((url) => {
          if (!active) {
            window.URL.revokeObjectURL(url);
            return;
          }
          if (fotoBlobRef.current) {
            window.URL.revokeObjectURL(fotoBlobRef.current);
          }
          fotoBlobRef.current = url;
          setFotoUrl(url);
        })
        .catch(() => {
          if (!active) return;
          if (fotoBlobRef.current) {
            window.URL.revokeObjectURL(fotoBlobRef.current);
            fotoBlobRef.current = null;
          }
          setFotoUrl(null);
        })
        .finally(() => {
          if (active) setCargandoFoto(false);
        });
    } else {
      if (fotoBlobRef.current) {
        window.URL.revokeObjectURL(fotoBlobRef.current);
        fotoBlobRef.current = null;
      }
      setFotoUrl(null);
    }

    return () => {
      active = false;
    };
  }, [trabajador?.tieneFoto, trabajadorId]);

  // Cleanup de blob al desmontar el componente
  useEffect(() => {
    return () => {
      if (fotoBlobRef.current) {
        window.URL.revokeObjectURL(fotoBlobRef.current);
        fotoBlobRef.current = null;
      }
    };
  }, []);

  if (isLoading) {
    return (
      <Flex justify="center" align="center" style={{ minHeight: 300 }}>
        <Spin size="large" />
      </Flex>
    );
  }

  if (isError || !trabajador) {
    return (
      <Flex vertical align="center" style={{ width: '100%', padding: '24px 0' }}>
        <Card style={{ width: '100%', maxWidth: 540 }}>
          <Alert
            type="error"
            showIcon
            message="No se pudo cargar el trabajador"
            description={getErrorMessage(error)}
            style={{ marginBottom: 16 }}
          />
          <Button type="primary" onClick={() => navigate(PATHS.trabajadores)}>
            Volver al listado
          </Button>
        </Card>
      </Flex>
    );
  }

  const nombreCompleto = `${trabajador.nombre} ${trabajador.apellido}`;

  // Guardar cambios: envía en lote todas las modificaciones confirmadas
  const onFinish = async (values: TrabajadorEditarRequest) => {
    setErrorArchivos(null);

    try {
      setGuardando(true);

      // 1. Modificar nombre y apellido si cambiaron
      if (values.nombre !== trabajador.nombre || values.apellido !== trabajador.apellido) {
        await editar.mutateAsync(values);
      }

      // 2. Foto: borrar o subir nueva
      if (fotoEliminada && trabajador.tieneFoto) {
        await eliminarFoto(trabajador.id);
      } else if (fotoNuevaFile) {
        await subirFoto(trabajador.id, fotoNuevaFile);
      }

      // 3. ART: borrar o subir nueva
      if (artEliminada && trabajador.documentos?.some((d) => d.tipo === 'ART')) {
        await eliminarDocumento(trabajador.id, 'ART');
      } else if (artNuevo) {
        await subirDocumento(trabajador.id, {
          tipo: 'ART',
          archivo: artNuevo.archivo,
          fechaVencimiento: artNuevo.fecha.format('YYYY-MM-DD'),
        });
      }

      // 4. Seguro de Vida: borrar o subir nuevo
      if (seguroEliminada && trabajador.documentos?.some((d) => d.tipo === 'SEGURO_VIDA')) {
        await eliminarDocumento(trabajador.id, 'SEGURO_VIDA');
      } else if (seguroNuevo) {
        await subirDocumento(trabajador.id, {
          tipo: 'SEGURO_VIDA',
          archivo: seguroNuevo.archivo,
          fechaVencimiento: seguroNuevo.fecha.format('YYYY-MM-DD'),
        });
      }

      await queryClient.invalidateQueries({ queryKey: ['trabajadores'] });
      message.success('Cambios guardados exitosamente');
      navigate(PATHS.trabajadores);
    } catch (err) {
      setGuardando(false);
      form.setFields(toFormFieldErrors(err));
      const errorMsg = getErrorMessage(err);
      setErrorArchivos(errorMsg);
      message.error(errorMsg);
    }
  };

  // Cargar / Reemplazar foto (guarda en borrador local)
  const handleFotoUpload = (file: File) => {
    const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png' || /\.(jpg|jpeg|png)$/i.test(file.name);
    if (!isJpgOrPng || file.size > 2 * 1024 * 1024) {
      setErrorArchivos('La foto debe ser JPG o PNG de hasta 2 MB');
      return Upload.LIST_IGNORE;
    }

    setErrorArchivos(null);
    setFotoNuevaFile(file);
    setFotoEliminada(false);
    const reader = new FileReader();
    reader.onload = (e) => setFotoNuevaPreview(e.target?.result as string);
    reader.readAsDataURL(file);
    return false;
  };

  // Borrar foto con modal de confirmación simétrico (guarda en borrador local)
  const handleAbrirBorrarFoto = () => {
    setModalBorrarFotoVisible(true);
  };

  const handleEjecutarBorrarFoto = () => {
    setFotoEliminada(true);
    setFotoNuevaFile(null);
    setFotoNuevaPreview(null);
    setModalBorrarFotoVisible(false);
  };

  // Descargar documento con loading y mensaje de error amigable (Item 5)
  const handleDescargarDocumento = async (tipo: TipoDocumento, nombreArchivo?: string) => {
    try {
      setDescargandoDoc(tipo);
      await descargarDocumento(trabajador.id, tipo, nombreArchivo);
    } catch {
      message.error(
        'No se pudo descargar el archivo: el archivo no existe en el servidor o hubo un error de conexión'
      );
    } finally {
      setDescargandoDoc(null);
    }
  };

  // Abrir modal para subir o renovar documento
  const handleAbrirModalDoc = (tipo: TipoDocumento) => {
    setModalDocTipo(tipo);
    setModalDocArchivo(null);
    setModalDocFecha(null);
    setModalDocError(null);
    setModalDocVisible(true);
  };

  // Confirmar archivo y fecha de documento (guarda en borrador local)
  const handleGuardarModalDoc = () => {
    if (!modalDocTipo) return;
    if (!modalDocArchivo) {
      setModalDocError('Debe seleccionar un archivo PDF');
      return;
    }
    if (!modalDocFecha) {
      setModalDocError('La fecha de vencimiento es obligatoria');
      return;
    }

    if (modalDocTipo === 'ART') {
      setArtNuevo({ archivo: modalDocArchivo, fecha: modalDocFecha });
      setArtEliminada(false);
    } else {
      setSeguroNuevo({ archivo: modalDocArchivo, fecha: modalDocFecha });
      setSeguroEliminada(false);
    }

    setModalDocVisible(false);
    setModalDocArchivo(null);
    setModalDocFecha(null);
    setModalDocError(null);
  };

  // Abrir modal de confirmación para eliminar documento
  const handleAbrirBorrarDoc = (tipo: TipoDocumento) => {
    setModalBorrarDocTipo(tipo);
  };

  // Ejecutar eliminación de documento (guarda en borrador local)
  const handleEjecutarBorrarDoc = () => {
    if (!modalBorrarDocTipo) return;
    if (modalBorrarDocTipo === 'ART') {
      setArtEliminada(true);
      setArtNuevo(null);
    } else {
      setSeguroEliminada(true);
      setSeguroNuevo(null);
    }
    setModalBorrarDocTipo(null);
  };

  const getDoc = (tipo: TipoDocumento) => trabajador.documentos?.find((d) => d.tipo === tipo);

  return (
    <Flex vertical align="center" style={{ width: '100%', padding: '24px 0' }}>
      <Card style={{ width: '100%', maxWidth: 540, borderRadius: 8 }}>
        <Typography.Title level={2} style={{ textAlign: 'center', marginTop: 0, marginBottom: 24 }}>
          {nombreCompleto}
        </Typography.Title>

        {(editar.isError || errorArchivos) && (
          <Alert
            type="error"
            showIcon
            message={errorArchivos || getErrorMessage(editar.error)}
            style={{ marginBottom: 20 }}
          />
        )}

        <Form
          form={form}
          layout="vertical"
          size="large"
          onFinish={onFinish}
          requiredMark={false}
        >
          {/* Nombre */}
          <Form.Item
            label="Nombre"
            name="nombre"
            rules={[
              { required: true, message: 'El nombre es obligatorio' },
              { max: 100, message: 'Máximo 100 caracteres' },
            ]}
          >
            <Input placeholder="Ingresá el nombre" />
          </Form.Item>

          {/* Apellido */}
          <Form.Item
            label="Apellido"
            name="apellido"
            rules={[
              { required: true, message: 'El apellido es obligatorio' },
              { max: 100, message: 'Máximo 100 caracteres' },
            ]}
          >
            <Input placeholder="Ingresá el apellido" />
          </Form.Item>

          {/* DNI (solo lectura, RN-11) */}
          <Form.Item label="DNI">
            <Input value={trabajador.dni} disabled />
          </Form.Item>

          <Divider style={{ margin: '20px 0' }} />

          {/* Sección Foto */}
          <Form.Item label="Foto de perfil">
            <Flex gap={16} align="center">
              {cargandoFoto && !fotoNuevaPreview ? (
                <Flex justify="center" align="center" style={{ width: 72, height: 72 }}>
                  <Spin />
                </Flex>
              ) : (
                <Avatar
                  size={72}
                  src={fotoEliminada ? null : (fotoNuevaPreview || fotoUrl)}
                  icon={((!fotoNuevaPreview && !fotoUrl) || fotoEliminada) ? <CameraOutlined /> : null}
                  style={{
                    backgroundColor: (!fotoEliminada && (fotoNuevaPreview || fotoUrl)) ? 'transparent' : palette.borderSoft,
                    color: palette.textSecondary,
                  }}
                />
              )}

              <Flex vertical gap={8} style={{ flex: 1 }}>
                <Flex gap={8}>
                  <Upload
                    accept=".jpg,.jpeg,.png"
                    showUploadList={false}
                    beforeUpload={handleFotoUpload}
                    className="upload-block"
                    style={{ flex: 1 }}
                  >
                    <Button
                      block
                      className="btn-upload-input"
                      icon={<UploadOutlined />}
                    >
                      {(!fotoEliminada && (Boolean(fotoNuevaPreview) || Boolean(trabajador.tieneFoto)))
                        ? 'Reemplazar foto'
                        : 'Subir foto'}
                    </Button>
                  </Upload>

                  {(!fotoEliminada && (Boolean(fotoNuevaPreview) || Boolean(trabajador.tieneFoto))) && (
                    <Button
                      danger
                      icon={<DeleteOutlined />}
                      onClick={handleAbrirBorrarFoto}
                    >
                      Borrar
                    </Button>
                  )}
                </Flex>
                {(fotoEliminada || (!fotoNuevaPreview && !trabajador.tieneFoto)) && (
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    * Dato opcional. De no añadirse, se solicitará DNI en Garita
                  </Typography.Text>
                )}
              </Flex>
            </Flex>
          </Form.Item>

          <Divider style={{ margin: '20px 0' }} />

          {/* Sección Documentación */}
          <Typography.Title level={5} style={{ marginTop: 0, marginBottom: 16 }}>
            Documentación
          </Typography.Title>

          {/* Póliza de ART */}
          <Card size="small" style={{ marginBottom: 16, background: palette.tableHeader }}>
            {(() => {
              const docOriginal = getDoc('ART');
              const tieneDoc = !artEliminada && (Boolean(artNuevo) || Boolean(docOriginal));
              const nombreArchivo = artNuevo ? artNuevo.archivo.name : docOriginal?.archivoNombre;
              const fechaVenc = artNuevo ? artNuevo.fecha : (docOriginal ? dayjs(docOriginal.fechaVencimiento) : null);
              const vencido = fechaVenc ? fechaVenc.isBefore(dayjs().startOf('day')) : false;

              return (
                <Flex vertical gap={8}>
                  <Flex justify="space-between" align="center">
                    <Typography.Text strong>Póliza de ART</Typography.Text>
                    <EstadoTag estado={tieneDoc ? (vencido ? 'VENCIDO' : 'VIGENTE') : 'PENDIENTE_CARGA'} />
                  </Flex>

                  {tieneDoc ? (
                    <>
                      <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                        Archivo: {nombreArchivo}
                      </Typography.Text>
                      <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                        Vencimiento: {fechaVenc?.format('DD/MM/YYYY')}
                      </Typography.Text>
                      <Flex gap={8} style={{ width: '100%', marginTop: 6 }}>
                        <Button
                          size="small"
                          style={{ flex: 1 }}
                          icon={<DownloadOutlined />}
                          loading={descargandoDoc === 'ART'}
                          onClick={() => {
                            if (artNuevo) {
                              const url = window.URL.createObjectURL(artNuevo.archivo);
                              const link = document.createElement('a');
                              link.href = url;
                              link.download = artNuevo.archivo.name;
                              document.body.appendChild(link);
                              link.click();
                              link.remove();
                              window.URL.revokeObjectURL(url);
                            } else if (docOriginal) {
                              handleDescargarDocumento('ART', docOriginal.archivoNombre);
                            }
                          }}
                        >
                          Descargar
                        </Button>
                        <Button
                          size="small"
                          style={{ flex: 1 }}
                          icon={<SyncOutlined />}
                          onClick={() => handleAbrirModalDoc('ART')}
                        >
                          Renovar
                        </Button>
                        <Button
                          size="small"
                          danger
                          style={{ flex: 1 }}
                          icon={<DeleteOutlined />}
                          onClick={() => handleAbrirBorrarDoc('ART')}
                        >
                          Borrar
                        </Button>
                      </Flex>
                    </>
                  ) : (
                    <Button
                      block
                      className="btn-upload-input"
                      icon={<FilePdfOutlined />}
                      onClick={() => handleAbrirModalDoc('ART')}
                    >
                      Subir archivo pdf
                    </Button>
                  )}
                </Flex>
              );
            })()}
          </Card>

          {/* Seguro de Vida */}
          <Card size="small" style={{ marginBottom: 24, background: palette.tableHeader }}>
            {(() => {
              const docOriginal = getDoc('SEGURO_VIDA');
              const tieneDoc = !seguroEliminada && (Boolean(seguroNuevo) || Boolean(docOriginal));
              const nombreArchivo = seguroNuevo ? seguroNuevo.archivo.name : docOriginal?.archivoNombre;
              const fechaVenc = seguroNuevo ? seguroNuevo.fecha : (docOriginal ? dayjs(docOriginal.fechaVencimiento) : null);
              const vencido = fechaVenc ? fechaVenc.isBefore(dayjs().startOf('day')) : false;

              return (
                <Flex vertical gap={8}>
                  <Flex justify="space-between" align="center">
                    <Typography.Text strong>Seguro de Vida</Typography.Text>
                    <EstadoTag estado={tieneDoc ? (vencido ? 'VENCIDO' : 'VIGENTE') : 'PENDIENTE_CARGA'} />
                  </Flex>

                  {tieneDoc ? (
                    <>
                      <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                        Archivo: {nombreArchivo}
                      </Typography.Text>
                      <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                        Vencimiento: {fechaVenc?.format('DD/MM/YYYY')}
                      </Typography.Text>
                      <Flex gap={8} style={{ width: '100%', marginTop: 6 }}>
                        <Button
                          size="small"
                          style={{ flex: 1 }}
                          icon={<DownloadOutlined />}
                          loading={descargandoDoc === 'SEGURO_VIDA'}
                          onClick={() => {
                            if (seguroNuevo) {
                              const url = window.URL.createObjectURL(seguroNuevo.archivo);
                              const link = document.createElement('a');
                              link.href = url;
                              link.download = seguroNuevo.archivo.name;
                              document.body.appendChild(link);
                              link.click();
                              link.remove();
                              window.URL.revokeObjectURL(url);
                            } else if (docOriginal) {
                              handleDescargarDocumento('SEGURO_VIDA', docOriginal.archivoNombre);
                            }
                          }}
                        >
                          Descargar
                        </Button>
                        <Button
                          size="small"
                          style={{ flex: 1 }}
                          icon={<SyncOutlined />}
                          onClick={() => handleAbrirModalDoc('SEGURO_VIDA')}
                        >
                          Renovar
                        </Button>
                        <Button
                          size="small"
                          danger
                          style={{ flex: 1 }}
                          icon={<DeleteOutlined />}
                          onClick={() => handleAbrirBorrarDoc('SEGURO_VIDA')}
                        >
                          Borrar
                        </Button>
                      </Flex>
                    </>
                  ) : (
                    <Button
                      block
                      className="btn-upload-input"
                      icon={<FilePdfOutlined />}
                      onClick={() => handleAbrirModalDoc('SEGURO_VIDA')}
                    >
                      Subir archivo pdf
                    </Button>
                  )}
                </Flex>
              );
            })()}
          </Card>

          {/* Botones principales */}
          <Flex gap={12} style={{ marginTop: 16 }}>
            <Button
              type="default"
              block
              onClick={() => navigate(PATHS.trabajadores)}
              disabled={guardando}
            >
              Descartar cambios
            </Button>
            <Button
              type="primary"
              htmlType="submit"
              block
              loading={guardando}
            >
              Guardar cambios
            </Button>
          </Flex>
        </Form>
      </Card>

      {/* Modal para Subir o Renovar Documento */}
      <Modal
        title={modalDocTipo === 'ART' ? 'Cargar / Renovar Póliza de ART' : 'Cargar / Renovar Seguro de Vida'}
        open={modalDocVisible}
        centered
        onCancel={() => setModalDocVisible(false)}
        destroyOnClose
        footer={
          <Flex gap={12} style={{ width: '100%', marginTop: 20 }}>
            <Button
              block
              style={{ flex: 1 }}
              size="large"
              onClick={() => setModalDocVisible(false)}
            >
              Cancelar
            </Button>
            <Button
              type="primary"
              block
              style={{ flex: 1 }}
              size="large"
              disabled={!modalDocArchivo || !modalDocFecha}
              onClick={handleGuardarModalDoc}
            >
              Cargar documento
            </Button>
          </Flex>
        }
      >
        <Flex vertical gap={16} style={{ marginTop: 16 }}>
          {modalDocError && <Alert type="error" showIcon message={modalDocError} />}

          <div>
            <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
              Archivo (PDF hasta 8 MB)
            </Typography.Text>
            <Upload
              accept=".pdf"
              maxCount={1}
              fileList={modalDocArchivo ? [{ uid: '-1', name: modalDocArchivo.name, status: 'done' }] : []}
              beforeUpload={(file) => {
                const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
                if (!isPdf || file.size > 8 * 1024 * 1024) {
                  setModalDocError('El archivo debe ser un PDF de hasta 8 MB');
                  return Upload.LIST_IGNORE;
                }
                setModalDocError(null);
                setModalDocArchivo(file);
                return false;
              }}
              onRemove={() => setModalDocArchivo(null)}
              className="upload-block"
            >
              <Button block className="btn-upload-input" icon={<UploadOutlined />}>
                {modalDocArchivo ? 'Cambiar archivo' : 'Subir archivo pdf'}
              </Button>
            </Upload>
          </div>

          <div>
            <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
              Indique fecha de vencimiento
            </Typography.Text>
            <DatePicker
              size="large"
              placeholder="DD/MM/AAAA"
              format="DD/MM/YYYY"
              value={modalDocFecha}
              onChange={(date) => setModalDocFecha(date)}
              style={{ width: '100%' }}
            />
          </div>
        </Flex>
      </Modal>

      {/* Modal de confirmación para borrar foto (simétrico según wireframe) */}
      <Modal
        open={modalBorrarFotoVisible}
        centered
        onCancel={() => setModalBorrarFotoVisible(false)}
        destroyOnClose
        closable={false}
        width={420}
        footer={
          <Flex gap={12} style={{ width: '100%', marginTop: 20 }}>
            <Button
              block
              style={{ flex: 1 }}
              size="large"
              onClick={() => setModalBorrarFotoVisible(false)}
            >
              Cancelar
            </Button>
            <Button
              danger
              type="primary"
              block
              style={{ flex: 1 }}
              size="large"
              onClick={handleEjecutarBorrarFoto}
            >
              Borrar foto
            </Button>
          </Flex>
        }
      >
        <Typography.Title
          level={4}
          style={{ textAlign: 'center', margin: '16px 0 8px 0', fontSize: 18 }}
        >
          ¿Seguro que quiere borrar la foto?
        </Typography.Title>
      </Modal>

      {/* Modal de confirmación para borrar documento (simétrico según wireframe) */}
      <Modal
        open={modalBorrarDocTipo !== null}
        centered
        onCancel={() => setModalBorrarDocTipo(null)}
        destroyOnClose
        closable={false}
        width={420}
        footer={
          <Flex gap={12} style={{ width: '100%', marginTop: 20 }}>
            <Button
              block
              style={{ flex: 1 }}
              size="large"
              onClick={() => setModalBorrarDocTipo(null)}
            >
              Cancelar
            </Button>
            <Button
              danger
              type="primary"
              block
              style={{ flex: 1 }}
              size="large"
              onClick={handleEjecutarBorrarDoc}
            >
              Eliminar documento
            </Button>
          </Flex>
        }
      >
        <Typography.Title
          level={4}
          style={{ textAlign: 'center', margin: '16px 0 8px 0', fontSize: 18 }}
        >
          ¿Seguro que desea eliminar este documento?
        </Typography.Title>
      </Modal>
    </Flex>
  );
}
