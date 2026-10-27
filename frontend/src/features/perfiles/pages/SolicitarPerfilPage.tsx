import { useEffect, useState } from 'react';
import { Alert, Button, Flex, Form, Input, Typography, Upload } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import { Link, useLocation, useNavigate } from 'react-router';
import { useLogout } from '../../auth/api/session';
import { useMiSolicitud, useSolicitarPerfil } from '../api/perfiles';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';

type TipoRegistro = 'EMPRESA' | 'MONOTRIBUTISTA';

interface FormValues {
  cuit?: string;
  razonSocial?: string;
  cuil?: string;
  nombre?: string;
  apellido?: string;
  dni?: string;
  telefono?: string;
}

/**
 * US-03: Paso 2 del registro. Declaración de datos fiscales y subida de constancia en PDF.
 * Cumple RF-08, RN-12 y RNF-10.
 */
export function SolicitarPerfilPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const logout = useLogout();
  const solicitarPerfil = useSolicitarPerfil();
  const { data: perfilExistente, isLoading: cargandoPerfil } = useMiSolicitud();
  const [form] = Form.useForm<FormValues>();

  // Si ya tiene una solicitud enviada, redirigir inmediatamente a estado-solicitud (US-08)
  useEffect(() => {
    if (perfilExistente) {
      navigate(PATHS.estadoSolicitud, { replace: true });
    }
  }, [perfilExistente, navigate]);

  // Si vino desde el paso 1 (RegistroPage), tipo está en location.state.tipo.
  // Se persiste en sessionStorage para que sobreviva recargas de página durante el flujo de registro.
  const tipoDesdeState = location.state && (location.state as { tipo?: TipoRegistro }).tipo;
  const tipoInicial = (): TipoRegistro => {
    if (tipoDesdeState === 'EMPRESA' || tipoDesdeState === 'MONOTRIBUTISTA') {
      sessionStorage.setItem('registro_tipo', tipoDesdeState);
      return tipoDesdeState;
    }
    const guardado = sessionStorage.getItem('registro_tipo');
    return guardado === 'MONOTRIBUTISTA' ? 'MONOTRIBUTISTA' : 'EMPRESA';
  };
  const tipo: TipoRegistro = tipoInicial();

  const [archivoSeleccionado, setArchivoSeleccionado] = useState<File | null>(null);
  const [errorArchivo, setErrorArchivo] = useState<string | null>(null);

  const esEmpresa = tipo === 'EMPRESA';

  const onFinish = (values: FormValues) => {
    setErrorArchivo(null);

    if (!archivoSeleccionado) {
      setErrorArchivo('Debe adjuntar la constancia de inscripción');
      return;
    }

    if (archivoSeleccionado.size > 8 * 1024 * 1024) {
      setErrorArchivo('El archivo debe ser un PDF de hasta 8 MB');
      return;
    }

    if (!archivoSeleccionado.name.toLowerCase().endsWith('.pdf')) {
      setErrorArchivo('El archivo debe ser un PDF de hasta 8 MB');
      return;
    }

    const formData = new FormData();
    formData.append('tipo', tipo);
    formData.append('constancia', archivoSeleccionado);

    if (values.telefono) {
      formData.append('telefono', values.telefono);
    }

    if (esEmpresa) {
      formData.append('cuit', values.cuit ?? '');
      formData.append('razonSocial', values.razonSocial ?? '');
    } else {
      formData.append('cuil', values.cuil ?? '');
      formData.append('nombre', values.nombre ?? '');
      formData.append('apellido', values.apellido ?? '');
      formData.append('dni', values.dni ?? '');
    }

    solicitarPerfil.mutate(formData, {
      onSuccess: () => {
        // Limpia el tipo guardado — el flujo de registro terminó
        sessionStorage.removeItem('registro_tipo');
        // Redirige al estado de la solicitud (US-08)
        navigate(PATHS.estadoSolicitud, { replace: true });
      },
    });
  };

  if (cargandoPerfil || perfilExistente) {
    return null;
  }

  return (
    <Flex vertical style={{ width: '100%', maxWidth: 440, margin: '0 auto' }}>
      <Typography.Title level={2} style={{ marginTop: 0, marginBottom: 28, textAlign: 'center' }}>
        {esEmpresa ? 'Datos de Empresa' : 'Datos de Monotributista'}
      </Typography.Title>

      {solicitarPerfil.isError && (
        <Alert
          type="error"
          showIcon
          title={getErrorMessage(solicitarPerfil.error)}
          style={{ marginBottom: 20 }}
        />
      )}

      {errorArchivo && (
        <Alert
          type="error"
          showIcon
          title={errorArchivo}
          style={{ marginBottom: 20 }}
        />
      )}

      <Form
        form={form}
        layout="vertical"
        size="large"
        requiredMark={false}
        onFinish={onFinish}
        onValuesChange={() => {
          if (solicitarPerfil.isError) solicitarPerfil.reset();
          if (errorArchivo) setErrorArchivo(null);
        }}
      >
        {esEmpresa ? (
          <>
            <Form.Item
              label="CUIT"
              name="cuit"
              rules={[
                { required: true, message: 'El CUIT es obligatorio' },
                {
                  pattern: /^\d{2}-?\d{8}-?\d$/,
                  message: 'Formato de CUIT no válido',
                },
              ]}
            >
              <Input placeholder="Ej. 20-12345678-9" />
            </Form.Item>

            <Form.Item
              label="Razón social"
              name="razonSocial"
              rules={[{ required: true, message: 'La razón social es obligatoria' }]}
            >
              <Input placeholder="Razón social" />
            </Form.Item>
          </>
        ) : (
          <>
            <Form.Item
              label="CUIL"
              name="cuil"
              rules={[
                { required: true, message: 'El CUIL es obligatorio' },
                {
                  pattern: /^\d{2}-?\d{8}-?\d$/,
                  message: 'Formato de CUIL no válido',
                },
              ]}
            >
              <Input placeholder="Ej. 20-12345678-9" />
            </Form.Item>

            <Form.Item
              label="Nombre"
              name="nombre"
              rules={[{ required: true, message: 'El nombre es obligatorio' }]}
            >
              <Input placeholder="Nombre" />
            </Form.Item>

            <Form.Item
              label="Apellido"
              name="apellido"
              rules={[{ required: true, message: 'El apellido es obligatorio' }]}
            >
              <Input placeholder="Apellido" />
            </Form.Item>

            <Form.Item
              label="DNI"
              name="dni"
              rules={[
                { required: true, message: 'El DNI es obligatorio' },
                { pattern: /^\d{7,8}$/, message: 'Formato de DNI no válido' },
              ]}
            >
              <Input placeholder="Ej. 12345678" />
            </Form.Item>
          </>
        )}

        <Form.Item label="Constancia de Inscripción" required>
          <Upload
            beforeUpload={(file) => {
              const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
              if (!isPdf) {
                setErrorArchivo('El archivo debe ser un PDF de hasta 8 MB');
                setArchivoSeleccionado(null);
                return Upload.LIST_IGNORE;
              }
              const isLt8M = file.size / 1024 / 1024 <= 8;
              if (!isLt8M) {
                setErrorArchivo('El archivo debe ser un PDF de hasta 8 MB');
                setArchivoSeleccionado(null);
                return Upload.LIST_IGNORE;
              }
              setErrorArchivo(null);
              setArchivoSeleccionado(file);
              return false; // Evita subida automática por AJAX
            }}
            fileList={archivoSeleccionado ? [{ uid: '-1', name: archivoSeleccionado.name, status: 'done' }] : []}
            onRemove={() => {
              setArchivoSeleccionado(null);
              setErrorArchivo(null);
            }}
            maxCount={1}
            accept=".pdf,application/pdf"
          >
            <Button icon={<UploadOutlined />} block size="large">
              Subir archivo pdf
            </Button>
          </Upload>
        </Form.Item>

        <Form.Item
          label="Número de teléfono"
          name="telefono"
          extra={<Typography.Text type="secondary" style={{ fontSize: 12 }}>* Dato opcional</Typography.Text>}
        >
          <Input placeholder="+54 9 1234 567890" />
        </Form.Item>

        <Flex vertical gap={12} style={{ marginTop: 20 }}>
          <Button type="primary" htmlType="submit" block loading={solicitarPerfil.isPending}>
            Registrarme
          </Button>
        </Flex>
      </Form>

      <Typography.Text style={{ textAlign: 'center', marginTop: 16 }} type="secondary">
        ¿Ya tenés una cuenta?{' '}
        <Link
          to={PATHS.login}
          onClick={(e) => {
            e.preventDefault();
            logout();
            navigate(PATHS.login);
          }}
        >
          Iniciá sesión
        </Link>
      </Typography.Text>
    </Flex>
  );
}
