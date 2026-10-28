import { useState } from 'react';
import { Alert, Button, Divider, Flex, Form, Input, Typography } from 'antd';
import { Link, useNavigate } from 'react-router';
import { useRegistro } from '../api/session';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';
import { CampoConfirmarPassword, CampoPasswordNueva } from '../../../shared/components/CamposPassword';

export type TipoRegistro = 'EMPRESA' | 'MONOTRIBUTISTA';

interface FormValues {
  email: string;
  password?: string;
  confirmarPassword?: string;
}

/**
 * US-02: registro paso 1 (elección de tipo y creación de cuenta con email y contraseña).
 * Cumple RF-08, RN-07, RN-12 y RNF-09.
 */
export function RegistroPage() {
  const navigate = useNavigate();
  const registro = useRegistro();
  const [form] = Form.useForm<FormValues>();

  // Sub-paso 1: elección de tipo ('EMPRESA' | 'MONOTRIBUTISTA')
  // Sub-paso 2: datos de usuario (email y contraseña)
  const [tipoSeleccionado, setTipoSeleccionado] = useState<TipoRegistro | null>(null);

  const onFinish = (values: FormValues) => {
    registro.mutate(
      {
        email: values.email,
        password: values.password!,
      },
      {
        onSuccess: () => {
          if (tipoSeleccionado) {
            sessionStorage.setItem('registro_tipo', tipoSeleccionado);
          }
          // Navega al paso 2 (US-03: solicitar perfil) pasando el tipo elegido en el estado
          navigate(PATHS.solicitarPerfil, {
            replace: true,
            state: { tipo: tipoSeleccionado },
          });
        },
      }
    );
  };

  /* ── SUB-PASO 1: elección de tipo ─────────────────────────────── */
  if (tipoSeleccionado === null) {
    return (
      <Flex vertical align="center" style={{ width: '100%', maxWidth: 480, margin: '0 auto' }}>
        <Typography.Title level={2} style={{ marginTop: 0, marginBottom: 8, textAlign: 'center' }}>
          Iniciar registro
        </Typography.Title>

        <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 8, fontWeight: 400, textAlign: 'center' }}>
          ¿De qué manera desea registrarse?
        </Typography.Title>

        <Typography.Paragraph type="secondary" style={{ textAlign: 'center', marginBottom: 32 }}>
          Tenga en cuenta que un <strong>monotributista</strong> no puede registrar
          otros trabajadores a su nombre
        </Typography.Paragraph>

        <Flex gap={16} justify="center" style={{ width: '100%', marginBottom: 24 }}>
          <Button
            type="primary"
            size="large"
            style={{ flex: 1, maxWidth: 200, height: 44 }}
            onClick={() => {
              sessionStorage.setItem('registro_tipo', 'EMPRESA');
              setTipoSeleccionado('EMPRESA');
            }}
          >
            Como <strong>Empresa</strong>
          </Button>
          <Button
            type="primary"
            size="large"
            style={{ flex: 1, maxWidth: 200, height: 44 }}
            onClick={() => {
              sessionStorage.setItem('registro_tipo', 'MONOTRIBUTISTA');
              setTipoSeleccionado('MONOTRIBUTISTA');
            }}
          >
            Como <strong>Monotributista</strong>
          </Button>
        </Flex>

        <Divider style={{ margin: '0 0 16px' }} />

        <Typography.Text type="secondary">
          ¿Ya tenés una cuenta?{' '}
          <Link to={PATHS.login}>Iniciá sesión</Link>
        </Typography.Text>
      </Flex>
    );
  }

  /* ── SUB-PASO 2: datos de usuario (email y contraseña) ─────────── */
  return (
    <Flex vertical style={{ width: '100%', maxWidth: 400, margin: '0 auto' }}>
      <Typography.Title level={2} style={{ marginTop: 0, marginBottom: 28, textAlign: 'center' }}>
        Datos de Usuario
      </Typography.Title>

      {registro.isError && (
        <Alert
          type="error"
          showIcon
          title={getErrorMessage(registro.error)}
          style={{ marginBottom: 20 }}
        />
      )}

      <Form
        form={form}
        layout="vertical"
        size="large"
        requiredMark={false}
        onFinish={onFinish}
        onValuesChange={() => registro.isError && registro.reset()}
      >
        <Form.Item
          label="Ingrese su dirección de Email"
          name="email"
          rules={[
            { required: true, message: 'El email es obligatorio' },
            { type: 'email', message: 'Formato de e-mail no válido' },
          ]}
        >
          <Input placeholder="Email" autoComplete="username" inputMode="email" />
        </Form.Item>

        {/* Componente estándar de US-44 con indicador de política */}
        <CampoPasswordNueva label="Cree una contraseña" placeholder="Contraseña" />

        <CampoConfirmarPassword label="Repita su contraseña" placeholder="Repetir contraseña" />

        <Flex vertical gap={12} style={{ marginTop: 24 }}>
          <Button type="primary" htmlType="submit" block loading={registro.isPending}>
            Continuar
          </Button>
        </Flex>
      </Form>

      <Divider style={{ margin: '16px 0 12px' }} />

      <Typography.Text style={{ textAlign: 'center' }} type="secondary">
        ¿Ya tenés una cuenta?{' '}
        <Link to={PATHS.login}>Iniciá sesión</Link>
      </Typography.Text>
    </Flex>
  );
}
