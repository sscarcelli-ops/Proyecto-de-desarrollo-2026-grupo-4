import { useEffect, useState } from 'react';
import {
  Alert,
  App,
  Button,
  Card,
  Col,
  Divider,
  Flex,
  Form,
  Input,
  Modal,
  Row,
  Spin,
  Typography,
  Upload,
} from 'antd';
import {
  CameraOutlined,
  DeleteOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import { type Dayjs } from 'dayjs';
import { useQueryClient } from '@tanstack/react-query';
import {
  descargarDocumento,
  eliminarDocumento,
  eliminarFoto,
  MONOTRIBUTISTA_PERFIL_KEY,
  obtenerFotoBlob,
  subirDocumento,
  subirFoto,
  useMiPerfilMonotributista,
  useModificarPerfilMonotributista,
  type MonotributistaPerfilModificarRequest,
  type TipoDocumento,
} from '../api/perfil';
import { palette } from '../../../app/theme';
import { EstadoTag, type Estado } from '../../../shared/components/EstadoTag';
import { getErrorMessage, toFormFieldErrors } from '../../../shared/lib/apiError';
import { ModalCargarDocumento } from '../../trabajadores/components/ModalCargarDocumento';
import { TarjetaDocumento } from '../components/TarjetaDocumento';
import { EncabezadoPagina } from '../../../shared/components/EncabezadoPagina';
import { usePantalla } from '../../../shared/hooks/usePantalla';
import type { ApiSchema } from '../../../shared/api/types';

type DocumentoResponse = ApiSchema<'DocumentoResponse'>;

/**
 * US-24: Completar el perfil propio del Monotributista (RF-09, RN-07).
 *
 * Permite a un Monotributista con perfil aprobado:
 * - Visualizar sus datos personales y estado documental.
 * - Modificar su Nombre y Apellido. DNI y CUIL son inmutables (CA-6).
 * - Cargar, reemplazar y eliminar su foto de perfil (JPG/PNG <= 2 MB).
 * - Cargar, renovar, descargar y eliminar su documentación obligatoria (ART y Seguro de Vida).
 */
export function DatosPersonalesPage() {
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [form] = Form.useForm();

  const { data: perfil, dataUpdatedAt, isLoading, isError, error } = useMiPerfilMonotributista();
  const { esCelular } = usePantalla();
  const modificarPerfil = useModificarPerfilMonotributista();

  // Foto guardada como blob autenticado con JWT
  const [fotoBlob, setFotoBlob] = useState<{ url: string | null } | null>(null);
  const [errorArchivos, setErrorArchivos] = useState<string | null>(null);
  const [descargandoDoc, setDescargandoDoc] = useState<TipoDocumento | null>(null);
  const [guardando, setGuardando] = useState(false);

  // Estados locales para borrador de cambios
  const [fotoNuevaFile, setFotoNuevaFile] = useState<File | null>(null);
  const [fotoNuevaPreview, setFotoNuevaPreview] = useState<string | null>(null);
  const [fotoEliminada, setFotoEliminada] = useState(false);

  const [artNuevo, setArtNuevo] = useState<{ archivo: File; fecha: Dayjs } | null>(null);
  const [artEliminada, setArtEliminada] = useState(false);

  const [seguroNuevo, setSeguroNuevo] = useState<{ archivo: File; fecha: Dayjs } | null>(null);
  const [seguroEliminada, setSeguroEliminada] = useState(false);

  // Modales de confirmación según wireframe (simétrico a TrabajadorEditarPage)
  const [modalBorrarFotoVisible, setModalBorrarFotoVisible] = useState(false);
  const [modalBorrarDocTipo, setModalBorrarDocTipo] = useState<TipoDocumento | null>(null);

  // Modal para carga/renovación de documento
  const [modalDocVisible, setModalDocVisible] = useState(false);
  const [modalDocTipo, setModalDocTipo] = useState<TipoDocumento | null>(null);

  // Sincronizar campos del formulario cuando llegan los datos del perfil
  useEffect(() => {
    if (perfil) {
      form.setFieldsValue({
        nombre: perfil.nombre,
        apellido: perfil.apellido,
      });
    }
  }, [perfil, form]);

  // Cargar foto autenticada con JWT mediante Blob si el perfil tiene foto
  const tieneFoto = perfil?.tieneFoto ?? false;
  useEffect(() => {
    if (!tieneFoto) return;
    let active = true;
    let urlCreada: string | null = null;

    obtenerFotoBlob()
      .then((url) => {
        if (!active) {
          window.URL.revokeObjectURL(url);
          return;
        }
        urlCreada = url;
        setFotoBlob({ url });
      })
      .catch(() => {
        if (active) setFotoBlob({ url: null });
      });

    return () => {
      active = false;
      if (urlCreada) window.URL.revokeObjectURL(urlCreada);
    };
  }, [tieneFoto, dataUpdatedAt]);

  if (isLoading) {
    return (
      <Flex justify="center" align="center" style={{ minHeight: 300 }}>
        <Spin size="large" />
      </Flex>
    );
  }

  if (isError || !perfil) {
    return (
      <Flex vertical align="center" style={{ width: '100%', padding: '24px 0' }}>
        <Card style={{ width: '100%', maxWidth: 540, borderRadius: 8 }}>
          <Alert
            type="error"
            showIcon
            title="No se pudieron cargar los datos personales"
            description={getErrorMessage(error)}
          />
        </Card>
      </Flex>
    );
  }

  const fotoActualUrl = fotoNuevaPreview ?? (fotoEliminada || !tieneFoto ? null : (fotoBlob?.url ?? null));

  // Manejo de cambio de foto (JPG/PNG <= 2 MB)
  const handleFotoChange = (file: File) => {
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

  // Modal de confirmación para borrar foto (simétrico según wireframe)
  const handleAbrirBorrarFoto = () => {
    setModalBorrarFotoVisible(true);
  };

  const handleEjecutarBorrarFoto = () => {
    setFotoEliminada(true);
    setFotoNuevaFile(null);
    setFotoNuevaPreview(null);
    setModalBorrarFotoVisible(false);
  };

  // Manejo de modal de documentos
  const handleAbrirModalDoc = (tipo: TipoDocumento) => {
    setModalDocTipo(tipo);
    setModalDocVisible(true);
  };

  const handleConfirmarModalDoc = (archivo: File, fecha: Dayjs) => {
    if (!modalDocTipo) return;
    if (modalDocTipo === 'ART') {
      setArtNuevo({ archivo, fecha });
      setArtEliminada(false);
    } else {
      setSeguroNuevo({ archivo, fecha });
      setSeguroEliminada(false);
    }
  };

  // Modal de confirmación para borrar documentos (simétrico según wireframe)
  const handleAbrirBorrarDoc = (tipo: TipoDocumento) => {
    setModalBorrarDocTipo(tipo);
  };

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

  // Descarga unificada de documentos (guardados en backend o borradores locales)
  const handleDescargarDoc = async (
    tipo: TipoDocumento,
    docOriginal?: DocumentoResponse,
    docNuevo?: { archivo: File } | null
  ) => {
    if (docNuevo) {
      const url = window.URL.createObjectURL(docNuevo.archivo);
      const link = document.createElement('a');
      link.href = url;
      link.download = docNuevo.archivo.name;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } else if (docOriginal) {
      try {
        setDescargandoDoc(tipo);
        await descargarDocumento(tipo, docOriginal.archivoNombre);
      } catch (err) {
        message.error(getErrorMessage(err));
      } finally {
        setDescargandoDoc(null);
      }
    }
  };

  const getDoc = (tipo: TipoDocumento) => perfil.documentos?.find((d) => d.tipo === tipo);

  // Guardar cambios personales y mutaciones pendientes
  const onFinish = async (values: MonotributistaPerfilModificarRequest) => {
    setErrorArchivos(null);
    let huboCambios = false;

    try {
      setGuardando(true);

      // 1. Modificar nombre o apellido si cambiaron
      const cambioDatos =
        values.nombre !== perfil.nombre ||
        values.apellido !== perfil.apellido;

      if (cambioDatos) {
        await modificarPerfil.mutateAsync(values);
        huboCambios = true;
      }

      // 2. Foto: eliminar o subir nueva
      if (fotoEliminada && perfil.tieneFoto) {
        await eliminarFoto();
        huboCambios = true;
      } else if (fotoNuevaFile) {
        await subirFoto(fotoNuevaFile);
        huboCambios = true;
      }

      // 3. ART: eliminar o subir nueva
      if (artEliminada && perfil.documentos?.some((d) => d.tipo === 'ART')) {
        await eliminarDocumento('ART');
        huboCambios = true;
      } else if (artNuevo) {
        await subirDocumento({
          tipo: 'ART',
          archivo: artNuevo.archivo,
          fechaVencimiento: artNuevo.fecha.format('YYYY-MM-DD'),
        });
        huboCambios = true;
      }

      // 4. Seguro de Vida: eliminar o subir nuevo
      if (seguroEliminada && perfil.documentos?.some((d) => d.tipo === 'SEGURO_VIDA')) {
        await eliminarDocumento('SEGURO_VIDA');
        huboCambios = true;
      } else if (seguroNuevo) {
        await subirDocumento({
          tipo: 'SEGURO_VIDA',
          archivo: seguroNuevo.archivo,
          fechaVencimiento: seguroNuevo.fecha.format('YYYY-MM-DD'),
        });
        huboCambios = true;
      }

      // Limpiar estados de borrador local
      setFotoNuevaFile(null);
      setFotoNuevaPreview(null);
      setFotoEliminada(false);
      setArtNuevo(null);
      setArtEliminada(false);
      setSeguroNuevo(null);
      setSeguroEliminada(false);

      await queryClient.invalidateQueries({ queryKey: MONOTRIBUTISTA_PERFIL_KEY });
      message.success('Datos personales actualizados correctamente');
    } catch (err) {
      form.setFields(toFormFieldErrors(err));
      const msg = getErrorMessage(err);
      setErrorArchivos(msg);
      message.error(msg);
    } finally {
      setGuardando(false);
      if (huboCambios) {
        queryClient.invalidateQueries({ queryKey: MONOTRIBUTISTA_PERFIL_KEY });
      }
    }
  };

  const handleDescartarCambios = () => {
    if (perfil) {
      form.setFieldsValue({
        nombre: perfil.nombre,
        apellido: perfil.apellido,
      });
    }
    setFotoNuevaFile(null);
    setFotoNuevaPreview(null);
    setFotoEliminada(false);
    setArtNuevo(null);
    setArtEliminada(false);
    setSeguroNuevo(null);
    setSeguroEliminada(false);
    message.info('Cambios descartados');
  };

  return (
    <div
      style={{
        maxWidth: 1080,
        margin: '0 auto',
        width: '100%',
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* Encabezado estándar T-07 con badge de estado documental */}
      <EncabezadoPagina
        titulo="Datos personales"
        descripcion="Completá y mantené actualizada tu información personal y documentación legal para ingresar a los barrios."
        accion={
          <Flex
            align="center"
            justify={esCelular ? 'space-between' : 'flex-end'}
            gap={10}
            style={{
              padding: '8px 16px',
              backgroundColor: palette.surface,
              borderRadius: 8,
              boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03)',
              border: `1px solid ${palette.borderSoft}`,
            }}
          >
            <Typography.Text type="secondary" style={{ fontSize: 13, fontWeight: 500 }}>
              Estado documental:
            </Typography.Text>
            <EstadoTag estado={perfil.estadoDocumental as Estado} />
          </Flex>
        }
      />

      {/* Alerta de errores de validación o servidor */}
      {(modificarPerfil.isError || errorArchivos) && (
        <Alert
          type="error"
          showIcon
          title={errorArchivos || getErrorMessage(modificarPerfil.error)}
          style={{ marginBottom: 20, borderRadius: 8 }}
        />
      )}

      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        requiredMark={false}
        style={{ width: '100%' }}
      >
        <Row gutter={[24, 24]}>
          {/* Columna Izquierda: Datos Personales, Fiscales y Documentación Legal */}
          <Col xs={24} lg={15}>
            <Card
              bordered
              style={{
                borderRadius: 8,
                backgroundColor: palette.surface,
                boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03)',
              }}
            >
              <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 16, color: palette.text }}>
                Información personal
              </Typography.Title>

              <Row gutter={16}>
                <Col xs={24} sm={12}>
                  <Form.Item
                    label="Nombre"
                    name="nombre"
                    rules={[
                      { required: true, message: 'El nombre es obligatorio' },
                      { max: 100, message: 'Máximo 100 caracteres' },
                    ]}
                  >
                    <Input placeholder="Ingresá tu nombre" maxLength={100} />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    label="Apellido"
                    name="apellido"
                    rules={[
                      { required: true, message: 'El apellido es obligatorio' },
                      { max: 100, message: 'Máximo 100 caracteres' },
                    ]}
                  >
                    <Input placeholder="Ingresá tu apellido" maxLength={100} />
                  </Form.Item>
                </Col>
              </Row>

              <Row gutter={16}>
                <Col xs={24} sm={12}>
                  <Form.Item
                    label="DNI"
                    extra="El DNI no puede modificarse por ser un identificador oficial."
                  >
                    <Input value={perfil.dni} disabled />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    label="CUIL"
                    extra="El CUIL no puede modificarse por ser un identificador fiscal."
                  >
                    <Input value={perfil.cuil} disabled />
                  </Form.Item>
                </Col>
              </Row>

              <Divider style={{ margin: '16px 0 20px 0' }} />

              <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 4, color: palette.text }}>
                Documentación legal
              </Typography.Title>
              <Typography.Paragraph type="secondary" style={{ fontSize: 13, marginBottom: 16 }}>
                Pólizas requeridas para contar con habilitación de ingreso a los barrios.
              </Typography.Paragraph>

              {/* Póliza de ART */}
              <TarjetaDocumento
                titulo="Póliza de ART"
                docOriginal={getDoc('ART')}
                docNuevo={artNuevo}
                eliminado={artEliminada}
                descargando={descargandoDoc === 'ART'}
                onCargar={() => handleAbrirModalDoc('ART')}
                onBorrar={() => handleAbrirBorrarDoc('ART')}
                onDescargar={() => handleDescargarDoc('ART', getDoc('ART'), artNuevo)}
              />

              {/* Seguro de Vida */}
              <TarjetaDocumento
                titulo="Seguro de Accidentes Personales / Vida"
                docOriginal={getDoc('SEGURO_VIDA')}
                docNuevo={seguroNuevo}
                eliminado={seguroEliminada}
                descargando={descargandoDoc === 'SEGURO_VIDA'}
                onCargar={() => handleAbrirModalDoc('SEGURO_VIDA')}
                onBorrar={() => handleAbrirBorrarDoc('SEGURO_VIDA')}
                onDescargar={() => handleDescargarDoc('SEGURO_VIDA', getDoc('SEGURO_VIDA'), seguroNuevo)}
              />
            </Card>
          </Col>

          {/* Columna Derecha: Foto de perfil (estilo wireframe) */}
          <Col xs={24} lg={9}>
            <Card
              bordered
              style={{
                borderRadius: 8,
                backgroundColor: palette.surface,
                boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03)',
              }}
            >
              <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 16, color: palette.text }}>
                Foto de perfil
              </Typography.Title>

              <Flex vertical align="center" gap={16}>
                {/* Cuadro de previsualización de la foto */}
                <div
                  style={{
                    width: 220,
                    height: 220,
                    borderRadius: 8,
                    border: `2px dashed ${fotoActualUrl ? palette.borderSoft : palette.border}`,
                    backgroundColor: fotoActualUrl ? '#ffffff' : palette.tableHeader,
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center',
                    overflow: 'hidden',
                  }}
                >
                  {fotoActualUrl ? (
                    <img
                      src={fotoActualUrl}
                      alt="Foto de perfil"
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                  ) : (
                    <Flex vertical align="center" gap={8} style={{ color: palette.textSecondary }}>
                      <CameraOutlined style={{ fontSize: 48, color: palette.textSecondary }} />
                      <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                        Sin foto cargada
                      </Typography.Text>
                    </Flex>
                  )}
                </div>

                {/* Acciones de foto: el botón "Subir foto" ocupa exactamente el mismo ancho total que el par "Cambiar foto" + "Eliminar" */}
                {fotoActualUrl ? (
                  <Flex gap={8} justify="center" align="center" style={{ width: 256, maxWidth: '100%' }}>
                    <Upload
                      accept="image/png,image/jpeg"
                      showUploadList={false}
                      beforeUpload={handleFotoChange}
                      className="upload-block"
                      style={{ flex: 1 }}
                    >
                      <Button block icon={<CameraOutlined />}>
                        Cambiar foto
                      </Button>
                    </Upload>
                    <Button
                      danger
                      icon={<DeleteOutlined />}
                      onClick={handleAbrirBorrarFoto}
                    >
                      Eliminar
                    </Button>
                  </Flex>
                ) : (
                  <Upload
                    accept="image/png,image/jpeg"
                    showUploadList={false}
                    beforeUpload={handleFotoChange}
                  >
                    <Button icon={<UploadOutlined />} style={{ width: 256 }}>
                      Subir foto
                    </Button>
                  </Upload>
                )}

                {/* Aclaración del wireframe */}
                <Typography.Text
                  type="secondary"
                  style={{ fontSize: 12, textAlign: 'center', lineHeight: 1.4, paddingInline: 8 }}
                >
                  * Dato opcional. De no añadirse, se solicitará DNI en Garita.
                </Typography.Text>
              </Flex>
            </Card>
          </Col>
        </Row>

        {/* Fila inferior de botones de acción centrados y de mayor tamaño */}
        <Flex justify="center" align="center" gap={20} wrap="wrap" style={{ marginTop: 28, marginBottom: 40 }}>
          <Button
            size="large"
            onClick={handleDescartarCambios}
            disabled={guardando || modificarPerfil.isPending}
            style={{
              minWidth: 220,
              height: 48,
              fontSize: 16,
              borderRadius: 6,
            }}
          >
            Descartar cambios
          </Button>
          <Button
            type="primary"
            size="large"
            htmlType="submit"
            loading={guardando || modificarPerfil.isPending}
            style={{
              minWidth: 220,
              height: 48,
              fontSize: 16,
              fontWeight: 600,
              borderRadius: 6,
            }}
          >
            Guardar cambios
          </Button>
        </Flex>
      </Form>

      {/* Modal para Subir o Renovar Documento (reutilizado) */}
      <ModalCargarDocumento
        open={modalDocVisible}
        tipo={modalDocTipo}
        onCancel={() => setModalDocVisible(false)}
        onConfirm={handleConfirmarModalDoc}
      />

      {/* Modal de confirmación para borrar foto (simétrico según wireframe) */}
      <Modal
        open={modalBorrarFotoVisible}
        centered
        onCancel={() => setModalBorrarFotoVisible(false)}
        destroyOnHidden
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
        destroyOnHidden
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
    </div>
  );
}
