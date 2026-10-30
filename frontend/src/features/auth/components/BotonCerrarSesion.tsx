import { useState, type CSSProperties } from 'react';
import { Button, Flex, Modal, Tooltip, Typography } from 'antd';
import { LogoutOutlined } from '@ant-design/icons';
import type { TooltipPlacement } from 'antd/es/tooltip';
import { useNavigate } from 'react-router';
import { useLogout } from '../api/session';
import { PATHS } from '../../../routes/paths';

/**
 * US-05 / RF-25: botón "Cerrar sesión" con la confirmación del wireframe. Lo usan el menú lateral (AppLayout),
 * la garita y las pantallas del registro, así todos cierran sesión igual. Solo cambia el estilo del botón.
 */
export function BotonCerrarSesion({
  style,
  ayudaEn = 'right',
  conTexto = false,
}: {
  style?: CSSProperties;
  // Dónde aparece la ayuda "Cerrar sesión": a la derecha en el menú lateral, abajo en el encabezado de la garita
  ayudaEn?: TooltipPlacement;
  // Link con el texto "Cerrar sesión" en lugar del ícono (pantallas sin menú: registro y estado de la solicitud)
  conTexto?: boolean;
}) {
  const navigate = useNavigate();
  const cerrarSesion = useLogout();
  const [confirmando, setConfirmando] = useState(false);

  const confirmar = () => {
    setConfirmando(false);
    cerrarSesion();
    navigate(PATHS.login, { replace: true });
  };

  return (
    <>
      {conTexto ? (
        <Button type="link" onClick={() => setConfirmando(true)} style={style}>
          Cerrar sesión
        </Button>
      ) : (
        <Tooltip title="Cerrar sesión" placement={ayudaEn}>
          <Button
            type="text"
            icon={<LogoutOutlined style={{ fontSize: 22 }} />}
            onClick={() => setConfirmando(true)}
            aria-label="Cerrar sesión"
            style={{ width: 44, height: 44, ...style }}
          />
        </Tooltip>
      )}

      <Modal
        open={confirmando}
        onCancel={() => setConfirmando(false)}
        footer={null}
        centered
        width={360}
        aria-labelledby="titulo-cerrar-sesion"
      >
        <Flex vertical align="center" gap={24} style={{ padding: '16px 8px 8px' }}>
          <Typography.Title id="titulo-cerrar-sesion" level={4} style={{ textAlign: 'center', margin: 0 }}>
            ¿Seguro que quiere Cerrar Sesión?
          </Typography.Title>
          <Flex vertical gap={12} style={{ width: '100%' }}>
            <Button size="large" block onClick={() => setConfirmando(false)}>
              Cancelar
            </Button>
            <Button size="large" type="primary" block onClick={confirmar} autoFocus>
              Aceptar
            </Button>
          </Flex>
        </Flex>
      </Modal>
    </>
  );
}
