import { Flex, Spin } from 'antd';
import { Navigate, Outlet } from 'react-router';
import { useCurrentUserQuery } from '../features/auth/api/useCurrentUser';
import { authToken } from '../shared/lib/authToken';
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
  const { isPending, isError } = useCurrentUserQuery();

  if (authToken.get() === null || isError) {
    return <Navigate to={PATHS.login} replace />;
  }
  if (isPending) {
    return <Cargando />;
  }
  return <Outlet />;
}

/** El login con una sesión abierta lleva directo a la pantalla de su rol. Si el token ya no sirve, muestra el login. */
export function RedirectIfAuthenticated() {
  const { data, isPending, isError } = useCurrentUserQuery();

  if (authToken.get() === null || isError) {
    return <Outlet />;
  }
  if (isPending) {
    return <Cargando />;
  }
  return <Navigate to={pantallaInicial(data.roles)} replace />;
}
