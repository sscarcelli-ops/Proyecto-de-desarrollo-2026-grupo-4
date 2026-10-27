import { Button, Flex, Typography } from 'antd';
import { ClockCircleOutlined } from '@ant-design/icons';
import { useLogout } from '../../auth/api/session';

/**
 * US-08: Vista de estado de la solicitud.
 * Mostrada cuando un usuario tiene su perfil PENDIENTE de aprobación.
 */
export function EstadoSolicitudPage() {
  const logout = useLogout();

  const cerrarSesion = () => {
    logout();
  };

  return (
    <Flex vertical align="center" justify="center" style={{ minHeight: '60vh', textAlign: 'center' }}>
      <ClockCircleOutlined style={{ fontSize: 64, color: '#1677ff', marginBottom: 24 }} />

      <Typography.Title level={2} style={{ marginBottom: 12 }}>
        Tu solicitud está pendiente de aprobación
      </Typography.Title>

      <Typography.Paragraph type="secondary" style={{ fontSize: 16, maxWidth: 500, marginBottom: 32 }}>
        Podrás utilizar las funcionalidades de tu perfil cuando el Administrador apruebe tu solicitud.
      </Typography.Paragraph>

      <Button type="default" onClick={cerrarSesion}>
        Cerrar sesión
      </Button>
    </Flex>
  );
}
