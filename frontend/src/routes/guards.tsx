import { Flex, Spin } from 'antd';
import { Navigate, Outlet } from 'react-router';
import { useCurrentUserQuery } from '../features/auth/api/useCurrentUser';
import { useMiSolicitud } from '../features/perfiles/api/perfiles';
import { useAuthToken } from '../shared/lib/authToken';
import { pantallaInicial } from './pantallaInicial';
import { PATHS } from './paths';

function Cargando() {
  return (
    <Flex justify="center" align="center" style={{ minHeight: '100vh' }}>
      <Spin size="large" />
    </Flex>
  );
}

/**
 * Pantallas con sesión (RF-22): sin token, o con uno que el backend rechaza (vencido, alterado), lleva al login.
 * El 401 ya borró el token (interceptor de httpClient), así que el login no vuelve a redirigir.
 * Los guards por rol (CA-5) se suman acá con las primeras pantallas de cada rol.
 */
export function RequireAuth() {
  // Se re-renderiza cuando el token desaparece en medio de la sesión (401 en una acción, logout en otra pestaña)
  const token = useAuthToken();
  const { isPending, isError } = useCurrentUserQuery();

  if (token === null || isError) {
    return <Navigate to={PATHS.login} replace />;
  }
  if (isPending) {
    return <Cargando />;
  }
  return <Outlet />;
}

/** El login con una sesión abierta lleva directo a la pantalla de su rol. Si el token ya no sirve, muestra el login. */
export function RedirectIfAuthenticated() {
  const token = useAuthToken();
  const { data, isPending, isError } = useCurrentUserQuery();
  const { data: perfil, isPending: pendingPerfil } = useMiSolicitud();

  if (token === null || isError) {
    return <Outlet />;
  }
  if (isPending || (token && pendingPerfil)) {
    return <Cargando />;
  }
  // Si no tiene roles todavía (cuenta en proceso de alta):
  // - Si ya envió su solicitud -> /estado-solicitud (US-08)
  // - Si todavía no completó los datos fiscales -> /solicitar-perfil (US-03)
  if (data.roles.length === 0) {
    if (perfil) {
      return <Navigate to={PATHS.estadoSolicitud} replace />;
    }
    return <Navigate to={PATHS.solicitarPerfil} replace />;
  }
  return <Navigate to={pantallaInicial(data.roles)} replace />;
}
