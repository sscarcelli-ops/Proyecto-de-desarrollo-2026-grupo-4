import { Flex, Spin } from 'antd';
import { Navigate, Outlet } from 'react-router';
import { useCurrentUserQuery } from '../features/auth/api/useCurrentUser';
import { useAuthToken } from '../shared/lib/authToken';
import { pantallaInicial } from './pantallaInicial';
import { PATHS } from './paths';
import type { Rol } from '../shared/types/rol';

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

/**
 * Restringe rutas a roles específicos (RF-22 / CA-5).
 * Si el usuario no tiene ninguno de los roles requeridos, se lo redirige a su pantalla inicial.
 */
export function RequireRole({ roles }: { roles: Rol[] }) {
  const { data: user, isPending } = useCurrentUserQuery();

  if (isPending) {
    return <Cargando />;
  }

  const tieneRol = user?.roles.some((r) => roles.includes(r)) ?? false;
  if (!tieneRol) {
    return <Navigate to={pantallaInicial(user?.roles ?? [])} replace />;
  }

  return <Outlet />;
}

/** El login con una sesión abierta lleva directo a la pantalla de su rol. Si el token ya no sirve, muestra el login. */
export function RedirectIfAuthenticated() {
  const token = useAuthToken();
  const { data, isPending, isError } = useCurrentUserQuery();

  if (token === null || isError) {
    return <Outlet />;
  }
  if (isPending) {
    return <Cargando />;
  }
  return <Navigate to={pantallaInicial(data.roles)} replace />;
}
