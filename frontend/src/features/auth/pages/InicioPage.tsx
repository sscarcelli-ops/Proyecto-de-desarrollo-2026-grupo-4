import { Typography } from 'antd';
import { useCurrentUser } from '../api/useCurrentUser';
import { ROL_LABEL } from '../../../shared/types/rol';

/**
 * Pantalla a la que lleva el login. Provisoria: cada rol tiene su propia pantalla principal según los
 * wireframes (Inicio del Administrador, Datos personales del Monotributista, garita del Guardia...), que se
 * agrega con las US de ese rol; un usuario sin rol aprobado verá el estado de su solicitud (US-08).
 */
export function InicioPage() {
  const user = useCurrentUser();
  if (!user) return null;

  return (
    <>
      <Typography.Title level={2} style={{ marginTop: 0 }}>Inicio</Typography.Title>
      <Typography.Paragraph type="secondary">
        Ingresaste como {user.nombre ?? user.email}
        {user.roles.length > 0 && ` (${user.roles.map((rol) => ROL_LABEL[rol]).join(' / ')})`}.
      </Typography.Paragraph>
    </>
  );
}
