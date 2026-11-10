import type { ComponentProps } from 'react';
import { Button, Dropdown } from 'antd';
import Icon, { MoreOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router';
import { palette } from '../../../app/theme';
import { PATHS } from '../../../routes/paths';

/** Llave del wireframe: el KeyOutlined de Ant Design, en tamaño chico, se confunde con una lupa. */
function LlaveSvg(props: ComponentProps<'svg'>) {
  return (
    <svg {...props} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
      <path d="M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z" />
      <circle cx="16.5" cy="7.5" r=".5" fill="currentColor" />
    </svg>
  );
}

const iconoLlave = <Icon component={LlaveSvg} style={{ fontSize: 18 }} />;

/**
 * US-07: acceso a "Cambiar contraseña", para todos los usuarios (también los que no tienen un rol aprobado).
 * Según el wireframe de cada lugar:
 * - puntos: tres puntos que abren la opción (menú lateral).
 * - boton: botón con la llave (búsqueda de la garita).
 * - link: link con el texto (pantallas sin menú: registro y estado de la solicitud).
 */
export function BotonCambiarPassword({ forma }: { forma: 'puntos' | 'boton' | 'link' }) {
  const navigate = useNavigate();
  const abrir = () => navigate(PATHS.cambiarPassword);

  if (forma === 'link') {
    return (
      <Button type="link" onClick={abrir}>
        Cambiar contraseña
      </Button>
    );
  }
  if (forma === 'boton') {
    return (
      <Button block icon={iconoLlave} onClick={abrir} style={{ height: 48, borderRadius: 6 }}>
        Cambiar Contraseña
      </Button>
    );
  }
  return (
    <Dropdown
      trigger={['click']}
      placement="topLeft"
      menu={{ items: [{ key: 'cambiar', icon: iconoLlave, label: 'Cambiar contraseña', onClick: abrir }] }}
    >
      <Button
        type="text"
        icon={<MoreOutlined style={{ fontSize: 22 }} />}
        aria-label="Más opciones"
        style={{ width: 44, height: 44, color: palette.sidebarText }}
      />
    </Dropdown>
  );
}
