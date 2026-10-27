import { Avatar, Flex, Typography } from 'antd';
import { Outlet } from 'react-router';
import { palette } from '../theme';

/**
 * Layout de las pantallas sin sesión (wireframes de login, activación de cuenta, registro y
 * recuperar contraseña): fondo gris claro, logo arriba a la izquierda y el contenido en una tarjeta blanca.
 */
export function PublicLayout() {
  return (
    <Flex vertical style={{ minHeight: '100vh', background: palette.background, padding: '24px 16px' }}>
      <Flex align="center" gap={12} style={{ marginBottom: 24 }}>
        {/* Reemplazar por el logo definitivo */}
        <Avatar size={44} />
        <Typography.Text strong style={{ fontSize: 18 }}>Logo</Typography.Text>
      </Flex>
      {/* Centrada en la pantalla: el padding de abajo compensa el alto de la fila del logo */}
      <Flex justify="center" align="center" style={{ flex: 1, paddingBottom: 116 }}>
        <div
          style={{
            width: '100%',
            maxWidth: 720,
            background: palette.surface,
            border: `1px solid ${palette.borderSoft}`,
            borderRadius: 8,
            padding: '48px 24px',
          }}
        >
          <Outlet />
        </div>
      </Flex>
    </Flex>
  );
}
