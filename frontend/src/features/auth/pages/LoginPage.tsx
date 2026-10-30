import { useEffect, useState } from 'react';
import { Alert, Button, Flex, Form, Input, Typography } from 'antd';
import { Link, useLocation, useNavigate } from 'react-router';
import { useLogin } from '../api/session';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { authToken } from '../../../shared/lib/authToken';
import { httpClient } from '../../../shared/lib/httpClient';
import type { ApiSchema } from '../../../shared/api/types';
import { PATHS } from '../../../routes/paths';
import { pantallaInicial } from '../../../routes/pantallaInicial';
import { toCurrentUser } from '../api/useCurrentUser';
import { useQueryClient } from '@tanstack/react-query';
import { MI_SOLICITUD_KEY } from '../../perfiles/api/perfiles';
import type { CuentaCreadaState } from './ActivacionPage';

/**
 * US-04: inicio de sesión (wireframe "Bienvenido"). Una sola pantalla para escritorio y celular: el formulario
 * tiene un ancho máximo y ocupa todo el ancho en pantallas chicas (Guardia en la garita, RNF-05).
 */
export function LoginPage() {
  const navigate = useNavigate();
  const login = useLogin();
  const queryClient = useQueryClient();
  // Se lee al entrar y se olvida: si recarga el login, el aviso ya no aparece
  const [sesionVencida] = useState(authToken.sesionVencida);
  useEffect(() => authToken.olvidarSesionVencida(), []);
  const cuentaCreada = (useLocation().state as CuentaCreadaState | null)?.cuentaCreada;

  const onFinish = (values: ApiSchema<'LoginRequest'>) =>
    login.mutate(values, {
      onSuccess: async (data) => {
        const user = toCurrentUser(data);
        if (user.roles.length === 0) {
          try {
            const perfilRes = await httpClient.get<ApiSchema<'PerfilResponse'>>('/perfil/mi-solicitud');
            queryClient.setQueryData(MI_SOLICITUD_KEY, perfilRes.data);
            navigate(pantallaInicial(user.roles, perfilRes.data), { replace: true });
          } catch {
            queryClient.setQueryData(MI_SOLICITUD_KEY, null);
            navigate(pantallaInicial(user.roles, null), { replace: true });
          }
        } else {
          navigate(pantallaInicial(user.roles), { replace: true });
        }
      },
    });

  return (
    <Flex vertical style={{ width: '100%', maxWidth: 360, margin: '0 auto' }}>
      <Typography.Title level={2} style={{ marginTop: 0, marginBottom: 32 }}>
        Bienvenido
      </Typography.Title>

      {/* El backend rechazó el token guardado (vencido o alterado): mismo mensaje para los dos casos */}
      {sesionVencida && login.isIdle && (
        <Alert
          type="warning"
          showIcon
          title="Tu sesión expiró. Iniciá sesión de nuevo."
          style={{ marginBottom: 24 }}
        />
      )}

      {/* Recién activó la cuenta del Administrador (US-01): el email ya queda cargado */}
      {cuentaCreada && login.isIdle && (
        <Alert type="success" showIcon title="Cuenta creada. Iniciá sesión." style={{ marginBottom: 24 }} />
      )}

      {/* Credenciales incorrectas (401) o cuenta que no puede operar (403): el mensaje viene del backend */}
      {login.isError && (
        <Alert type="error" showIcon title={getErrorMessage(login.error)} style={{ marginBottom: 24 }} />
      )}

      <Form
        layout="vertical"
        size="large"
        initialValues={{ email: cuentaCreada }}
        // Sin "*": en un login es obvio que los dos campos son obligatorios
        requiredMark={false}
        onFinish={onFinish}
        onValuesChange={() => login.isError && login.reset()}
      >
        <Form.Item
          label="Email"
          name="email"
          rules={[
            { required: true, message: 'El email es obligatorio' },
            { type: 'email', message: 'Ingresá un email válido' },
          ]}
        >
          <Input placeholder="Ingresá tu email" autoComplete="username" inputMode="email" />
        </Form.Item>

        <Form.Item
          label="Contraseña"
          name="password"
          rules={[{ required: true, message: 'La contraseña es obligatoria' }]}
          style={{ marginBottom: 8 }}
        >
          <Input.Password placeholder="Ingresá tu contraseña" autoComplete="current-password" />
        </Form.Item>

        <Flex justify="flex-end" style={{ marginBottom: 24 }}>
          <Link to={PATHS.recuperarPassword}>¿Olvidaste tu contraseña?</Link>
        </Flex>

        <Button type="primary" htmlType="submit" block loading={login.isPending}>
          Ingresar
        </Button>
      </Form>

      {/* Solo Empresas y Monotributistas se registran solos (RF-08); Operadores y Guardias los crea otro */}
      <Flex vertical align="center" gap={4} style={{ marginTop: 16 }}>
        <Typography.Text type="secondary">¿No tenés cuenta?</Typography.Text>
        <Link to={PATHS.registro}>Registrate como empresa o monotributista</Link>
      </Flex>
    </Flex>
  );
}
