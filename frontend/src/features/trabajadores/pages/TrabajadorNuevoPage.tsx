import { useState } from 'react';
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
  Typography,
  Upload,
} from 'antd';
import {
  CameraOutlined,
  DeleteOutlined,
  EditOutlined,
  FilePdfOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router';
import dayjs, { type Dayjs } from 'dayjs';
import {
  subirDocumento,
  subirFoto,
  useCrearTrabajador,
  type TrabajadorRequest,
} from '../api/trabajadores';
import { getErrorMessage, toFormFieldErrors } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';
import { palette } from '../../../app/theme';

/**
 * US-25: Registrar trabajador.
 *
 * En S1 se registran los datos básicos (nombre, apellido, DNI).
 * En S2 (US-28) se incorporan:
 * - Carga de foto de perfil (JPG/PNG <= 2 MB, opcional con advertencia de DNI en Garita).
 * - Carga de constancia de ART con selector de vencimiento (PDF <= 8 MB, opcional).
 * - Carga de Seguro de Vida con selector de vencimiento (PDF <= 8 MB, opcional).
 */
export function TrabajadorNuevoPage() {
  const navigate = useNavigate();
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const crear = useCrearTrabajador();

  // Estados locales para los archivos adjuntos opcionales
  const [fotoFile, setFotoFile] = useState<File | null>(null);
  const [fotoPreview, setFotoPreview] = useState<string | null>(null);

  const [artFile, setArtFile] = useState<File | null>(null);
  const [artFecha, setArtFecha] = useState<Dayjs | null>(null);

  const [seguroFile, setSeguroFile] = useState<File | null>(null);
  const [seguroFecha, setSeguroFecha] = useState<Dayjs | null>(null);

  // Estado para el modal de fecha de vencimiento (patrón de los wireframes)
  const [modalDocTipo, setModalDocTipo] = useState<'ART' | 'SEGURO_VIDA' | null>(null);
  const [modalPendingFile, setModalPendingFile] = useState<File | null>(null);
  const [modalFecha, setModalFecha] = useState<Dayjs | null>(null);

  const [errorArchivos, setErrorArchivos] = useState<string | null>(null);
  const [guardandoArchivos, setGuardandoArchivos] = useState(false);

  // Manejo de la foto
  const handleFotoChange = (file: File) => {
    const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png' || /\.(jpg|jpeg|png)$/i.test(file.name);
    if (!isJpgOrPng || file.size > 2 * 1024 * 1024) {
      setErrorArchivos('La foto debe ser JPG o PNG de hasta 2 MB');
      return Upload.LIST_IGNORE;
    }

    setErrorArchivos(null);
    setFotoFile(file);
    const reader = new FileReader();
    reader.onload = (e) => setFotoPreview(e.target?.result as string);
    reader.readAsDataURL(file);
    return false;
  };

  const handleQuitarFoto = () => {
    setFotoFile(null);
    setFotoPreview(null);
  };

  // Manejo de ART (PDF) - Dispara el modal para solicitar fecha de vencimiento
  const handleArtChange = (file: File) => {
    const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
    if (!isPdf || file.size > 8 * 1024 * 1024) {
      setErrorArchivos('El archivo debe ser un PDF de hasta 8 MB');
      return Upload.LIST_IGNORE;
    }
    setErrorArchivos(null);
    setModalPendingFile(file);
    setModalFecha(artFecha || null);
    setModalDocTipo('ART');
    return false;
  };

  // Manejo de Seguro de Vida (PDF) - Dispara el modal para solicitar fecha de vencimiento
  const handleSeguroChange = (file: File) => {
    const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
    if (!isPdf || file.size > 8 * 1024 * 1024) {
      setErrorArchivos('El archivo debe ser un PDF de hasta 8 MB');
      return Upload.LIST_IGNORE;
    }
    setErrorArchivos(null);
    setModalPendingFile(file);
    setModalFecha(seguroFecha || null);
    setModalDocTipo('SEGURO_VIDA');
    return false;
  };

  // Abrir modal para editar únicamente la fecha de vencimiento
  const handleAbrirEditarFecha = (tipo: 'ART' | 'SEGURO_VIDA') => {
    if (tipo === 'ART' && artFile) {
      setModalPendingFile(artFile);
      setModalFecha(artFecha);
      setModalDocTipo('ART');
    } else if (tipo === 'SEGURO_VIDA' && seguroFile) {
      setModalPendingFile(seguroFile);
      setModalFecha(seguroFecha);
      setModalDocTipo('SEGURO_VIDA');
    }
  };

  // Confirmar fecha en el modal
  const handleConfirmarModalFecha = () => {
    if (!modalFecha) return;

    if (modalDocTipo === 'ART') {
      if (modalPendingFile) setArtFile(modalPendingFile);
      setArtFecha(modalFecha);
    } else if (modalDocTipo === 'SEGURO_VIDA') {
      if (modalPendingFile) setSeguroFile(modalPendingFile);
      setSeguroFecha(modalFecha);
    }

    setModalDocTipo(null);
    setModalPendingFile(null);
    setModalFecha(null);
  };

  // Cancelar en el modal (si no había archivo confirmado antes, descarta la selección)
  const handleCancelarModalFecha = () => {
    setModalDocTipo(null);
    setModalPendingFile(null);
    setModalFecha(null);
  };

  const onFinish = async (values: TrabajadorRequest) => {
    setErrorArchivos(null);

    // Validación de fecha de vencimiento si hay archivo seleccionado (RN-04)
    if (artFile && !artFecha) {
      setErrorArchivos('La fecha de vencimiento es obligatoria para la ART adjunta');
      return;
    }

    if (seguroFile && !seguroFecha) {
      setErrorArchivos('La fecha de vencimiento es obligatoria para el Seguro de Vida adjunto');
      return;
    }

    let nuevoTrabajadorId: number | null = null;

    try {
      setGuardandoArchivos(true);

      // Paso 1: Crear el trabajador con nombre, apellido y DNI (JSON puro)
      const trabajador = await crear.mutateAsync(values);
      nuevoTrabajadorId = trabajador.id;

      // Paso 2: Subir foto si fue seleccionada (multipart)
      if (fotoFile) {
        await subirFoto(trabajador.id, fotoFile);
      }

      // Paso 3: Subir ART si fue seleccionada
      if (artFile && artFecha) {
        await subirDocumento(trabajador.id, {
          tipo: 'ART',
          archivo: artFile,
          fechaVencimiento: artFecha.format('YYYY-MM-DD'),
        });
      }

      // Paso 4: Subir Seguro de Vida si fue seleccionado
      if (seguroFile && seguroFecha) {
        await subirDocumento(trabajador.id, {
          tipo: 'SEGURO_VIDA',
          archivo: seguroFile,
          fechaVencimiento: seguroFecha.format('YYYY-MM-DD'),
        });
      }

      message.success('Trabajador registrado exitosamente');
      navigate(PATHS.trabajadores);
    } catch (error) {
      setGuardandoArchivos(false);
      const errorMsg = getErrorMessage(error);

      // Si el trabajador ya fue creado pero falló la subida de algún archivo (C-2):
      // Redirigir a la pantalla de edición para que el usuario no intente crear un duplicado
      if (nuevoTrabajadorId) {
        message.warning(
          `Trabajador registrado, pero hubo un problema al subir los archivos: ${errorMsg}. Podés completarlos desde la edición.`
        );
        navigate(`/trabajadores/${nuevoTrabajadorId}/editar`);
        return;
      }

      form.setFields(toFormFieldErrors(error));
      setErrorArchivos(errorMsg);
      message.error(errorMsg);
    }
  };

  const isSubmitting = crear.isPending || guardandoArchivos;

  return (
    <Flex vertical align="center" style={{ width: '100%', padding: '24px 0' }}>
      <Card style={{ width: '100%', maxWidth: 540, borderRadius: 8 }}>
        <Typography.Title level={2} style={{ textAlign: 'center', marginTop: 0, marginBottom: 24 }}>
          Nuevo trabajador
        </Typography.Title>

        {(crear.isError || errorArchivos) && (
          <Alert
            type="error"
            showIcon
            title={errorArchivos || getErrorMessage(crear.error)}
            style={{ marginBottom: 20 }}
          />
        )}

        <Form
          form={form}
          layout="vertical"
          size="large"
          onFinish={onFinish}
          requiredMark="optional"
        >
          {/* Datos básicos */}
          <Form.Item
            label="Nombre"
            name="nombre"
            rules={[
              { required: true, message: 'El nombre es obligatorio' },
              { max: 100, message: 'Máximo 100 caracteres' },
            ]}
          >
            <Input placeholder="Ingresá el nombre" maxLength={100} />
          </Form.Item>

          <Form.Item
            label="Apellido"
            name="apellido"
            rules={[
              { required: true, message: 'El apellido es obligatorio' },
              { max: 100, message: 'Máximo 100 caracteres' },
            ]}
          >
            <Input placeholder="Ingresá el apellido" maxLength={100} />
          </Form.Item>

          <Form.Item
            label="DNI"
            name="dni"
            rules={[
              { required: true, message: 'El DNI es obligatorio' },
              { pattern: /^[0-9]{7,8}$/, message: 'Ingresá un DNI válido de 7 u 8 dígitos (sin puntos)' },
            ]}
          >
            <Input placeholder="Sin puntos" maxLength={8} inputMode="numeric" />
          </Form.Item>

          <Divider style={{ margin: '20px 0' }} />

          {/* Foto de perfil (opcional) */}
          <Form.Item label="Foto de perfil">
            <Flex gap={16} align="center">
              <Avatar
                size={72}
                src={fotoPreview}
                icon={!fotoPreview && <CameraOutlined />}
                style={{
                  backgroundColor: fotoPreview ? 'transparent' : palette.borderSoft,
                  color: palette.textSecondary,
                  flexShrink: 0,
                }}
              />
              <Flex vertical gap={8} style={{ flex: 1 }}>
                {fotoPreview ? (
                  <Flex gap={8}>
                    <Upload
                      accept=".jpg,.jpeg,.png"
                      showUploadList={false}
                      beforeUpload={handleFotoChange}
                      className="upload-block"
                      style={{ flex: 1 }}
                    >
                      <Button block className="btn-upload-input" icon={<UploadOutlined />}>
                        Cambiar foto
                      </Button>
                    </Upload>
                    <Button
                      danger
                      icon={<DeleteOutlined />}
                      onClick={handleQuitarFoto}
                    >
                      Quitar
                    </Button>
                  </Flex>
                ) : (
                  <Upload
                    accept=".jpg,.jpeg,.png"
                    showUploadList={false}
                    beforeUpload={handleFotoChange}
                    className="upload-block"
                  >
                    <Button block className="btn-upload-input" icon={<UploadOutlined />}>
                      Subir foto
                    </Button>
                  </Upload>
                )}
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  * Dato opcional. De no añadirse, se solicitará DNI en Garita
                </Typography.Text>
              </Flex>
            </Flex>
          </Form.Item>

          <Divider style={{ margin: '20px 0' }} />

          {/* Documentación */}
          <Typography.Title level={5} style={{ margin: 0, marginBottom: 4 }}>
            Documentación
          </Typography.Title>
          <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 16 }}>
            Podés registrar al trabajador ahora y adjuntar o renovar los comprobantes más adelante. La documentación es obligatoria para su ingreso en garita.
          </Typography.Text>

          {/* Bloque ART */}
          <Card size="small" style={{ marginBottom: 16, background: palette.tableHeader }}>
            <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
              Póliza de ART
            </Typography.Text>
            {artFile ? (
              <Flex vertical gap={8}>
                <Flex gap={8}>
                  <Upload
                    accept=".pdf"
                    showUploadList={false}
                    beforeUpload={handleArtChange}
                    className="upload-block"
                    style={{ flex: 1 }}
                  >
                    <Button block className="btn-upload-input" icon={<FilePdfOutlined />}>
                      Reemplazar PDF
                    </Button>
                  </Upload>
                  <Button
                    danger
                    icon={<DeleteOutlined />}
                    onClick={() => {
                      setArtFile(null);
                      setArtFecha(null);
                    }}
                  >
                    Quitar
                  </Button>
                </Flex>
                <Flex justify="space-between" align="center" style={{ fontSize: 13, marginTop: 4 }}>
                  <Typography.Text ellipsis style={{ maxWidth: '60%' }}>
                    📎 {artFile.name}
                  </Typography.Text>
                  <Button
                    type="link"
                    size="small"
                    icon={<EditOutlined />}
                    onClick={() => handleAbrirEditarFecha('ART')}
                    style={{ padding: 0, height: 'auto', fontSize: 13 }}
                  >
                    Vence: {artFecha?.format('DD/MM/YYYY')}
                  </Button>
                </Flex>
              </Flex>
            ) : (
              <Upload
                accept=".pdf"
                showUploadList={false}
                beforeUpload={handleArtChange}
                className="upload-block"
              >
                <Button block className="btn-upload-input" icon={<FilePdfOutlined />}>
                  Subir archivo pdf
                </Button>
              </Upload>
            )}
            {!artFile && (
              <Typography.Text type="secondary" style={{ fontSize: 12, marginTop: 8, display: 'block' }}>
                * Opcional al registrar. Obligatorio para ingreso en garita
              </Typography.Text>
            )}
          </Card>

          {/* Bloque Seguro de Vida */}
          <Card size="small" style={{ marginBottom: 20, background: palette.tableHeader }}>
            <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
              Seguro de Vida
            </Typography.Text>
            {seguroFile ? (
              <Flex vertical gap={8}>
                <Flex gap={8}>
                  <Upload
                    accept=".pdf"
                    showUploadList={false}
                    beforeUpload={handleSeguroChange}
                    className="upload-block"
                    style={{ flex: 1 }}
                  >
                    <Button block className="btn-upload-input" icon={<FilePdfOutlined />}>
                      Reemplazar PDF
                    </Button>
                  </Upload>
                  <Button
                    danger
                    icon={<DeleteOutlined />}
                    onClick={() => {
                      setSeguroFile(null);
                      setSeguroFecha(null);
                    }}
                  >
                    Quitar
                  </Button>
                </Flex>
                <Flex justify="space-between" align="center" style={{ fontSize: 13, marginTop: 4 }}>
                  <Typography.Text ellipsis style={{ maxWidth: '60%' }}>
                    📎 {seguroFile.name}
                  </Typography.Text>
                  <Button
                    type="link"
                    size="small"
                    icon={<EditOutlined />}
                    onClick={() => handleAbrirEditarFecha('SEGURO_VIDA')}
                    style={{ padding: 0, height: 'auto', fontSize: 13 }}
                  >
                    Vence: {seguroFecha?.format('DD/MM/YYYY')}
                  </Button>
                </Flex>
              </Flex>
            ) : (
              <Upload
                accept=".pdf"
                showUploadList={false}
                beforeUpload={handleSeguroChange}
                className="upload-block"
              >
                <Button block className="btn-upload-input" icon={<FilePdfOutlined />}>
                  Subir archivo pdf
                </Button>
              </Upload>
            )}
            {!seguroFile && (
              <Typography.Text type="secondary" style={{ fontSize: 12, marginTop: 8, display: 'block' }}>
                * Opcional al registrar. Obligatorio para ingreso en garita
              </Typography.Text>
            )}
          </Card>

          {/* Botones de acción */}
          <Flex vertical align="center" gap={16} style={{ marginTop: 24 }}>
            <Button
              type="primary"
              htmlType="submit"
              block
              loading={isSubmitting}
            >
              Registrar trabajador
            </Button>
            <Typography.Link
              onClick={() => navigate(PATHS.trabajadores)}
              style={{ fontSize: 13, color: palette.textSecondary }}
            >
              Volver
            </Typography.Link>
          </Flex>
        </Form>
      </Card>

      {/* Modal para indicar fecha de vencimiento (wireframe) */}
      <Modal
        title="Indique fecha de vencimiento"
        open={modalDocTipo !== null}
        centered
        onCancel={handleCancelarModalFecha}
        destroyOnHidden
        footer={
          <Flex gap={12} style={{ width: '100%', marginTop: 20 }}>
            <Button
              block
              style={{ flex: 1 }}
              size="large"
              onClick={handleCancelarModalFecha}
            >
              Cancelar
            </Button>
            <Button
              type="primary"
              block
              style={{ flex: 1 }}
              size="large"
              disabled={!modalFecha}
              onClick={handleConfirmarModalFecha}
            >
              Aceptar
            </Button>
          </Flex>
        }
      >
        <Flex vertical gap={12} style={{ marginTop: 16 }}>
          {modalPendingFile && (
            <Typography.Text type="secondary" ellipsis style={{ fontSize: 13 }}>
              Archivo: {modalPendingFile.name}
            </Typography.Text>
          )}
          <DatePicker
            size="large"
            format="DD/MM/YYYY"
            placeholder="DD/MM/AAAA"
            value={modalFecha}
            onChange={(date) => setModalFecha(date)}
            style={{ width: '100%' }}
            autoFocus
          />
          {modalFecha && modalFecha.isBefore(dayjs().startOf('day')) && (
            <Alert
              type="warning"
              showIcon
              title="La fecha ingresada ya venció. El estado documental quedará en 'Vencido' y no habilitará el ingreso en garita hasta que se renueve."
              style={{ fontSize: 12 }}
            />
          )}
        </Flex>
      </Modal>
    </Flex>
  );
}
