import { Flex, Spin } from 'antd';
import { Navigate, Outlet } from 'react-router';
import { useCurrentUserQuery } from '../features/auth/api/useCurrentUser';
import { useMiSolicitud } from '../features/perfiles/api/perfiles';
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

/** Lleva a la pantalla inicial del usuario. Sin rol, depende de si ya envió la solicitud. */
function IrAPantallaInicial() {
  const { data: user } = useCurrentUserQuery();
  const sinRol = user?.roles.length === 0;
  const { data: perfil, isPending } = useMiSolicitud({ enabled: sinRol });

  if (sinRol && isPending) {
    return <Cargando />;
  }
  return <Navigate to={pantallaInicial(user?.roles ?? [], perfil)} replace />;
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
    return <IrAPantallaInicial />;
  }

  return <Outlet />;
}

/** US-43: las pantallas con menú son solo para cuentas con rol. Sin rol: su solicitud (RF-22, RN-14). */
export function RequireRolAprobado() {
  const { data: user } = useCurrentUserQuery();
  return user?.roles.length === 0 ? <IrAPantallaInicial /> : <Outlet />;
}

/** US-43: completar o ver la solicitud es solo para cuentas sin rol. Con rol ya está aprobada: va a su pantalla. */
export function RequireSinRol() {
  const { data: user } = useCurrentUserQuery();
  return user?.roles.length === 0 ? <Outlet /> : <IrAPantallaInicial />;
}

/** El estado de la solicitud necesita una solicitud: sin ella, va a completarla (US-03 CA-5). */
export function RequireSolicitud() {
  const { data: perfil, isPending } = useMiSolicitud();

  if (isPending) {
    return <Cargando />;
  }
  return perfil ? <Outlet /> : <Navigate to={PATHS.solicitarPerfil} replace />;
}

/** El login con una sesión abierta lleva directo a la pantalla de su rol. Si el token ya no sirve, muestra el login. */
export function RedirectIfAuthenticated() {
  const token = useAuthToken();
  const { data, isPending, isError } = useCurrentUserQuery();
  const { data: perfil, isPending: pendingPerfil } = useMiSolicitud({ enabled: data?.roles.length === 0 });

  if (token === null || isError) {
    return <Outlet />;
  }
  if (isPending || (token && data?.roles.length === 0 && pendingPerfil)) {
    return <Cargando />;
  }
  // Si no tiene roles todavía (cuenta en proceso de alta):
  // pantallaInicial se encarga de decidir si va a /estado-solicitud o a /solicitar-perfil
  return <Navigate to={pantallaInicial(data.roles, perfil)} replace />;
}
