import { createBrowserRouter } from 'react-router';
import { PATHS } from './paths';
import { NotFoundPage } from './NotFoundPage';
import { RedirectIfAuthenticated, RequireAuth } from './guards';
import { AppLayout } from '../app/layout/AppLayout';
import { PublicLayout } from '../app/layout/PublicLayout';
import { LoginPage } from '../features/auth/pages/LoginPage';
import { RegistroPage } from '../features/auth/pages/RegistroPage';
import { InicioPage } from '../features/auth/pages/InicioPage';
import { SolicitarPerfilPage } from '../features/perfiles/pages/SolicitarPerfilPage';
import { EstadoSolicitudPage } from '../features/perfiles/pages/EstadoSolicitudPage';
import { PingPage } from '../features/health/pages/PingPage';

export const router = createBrowserRouter([
  // Pantallas sin sesión: login (US-04), registro (US-02).
  // Se suman activación del Administrador (US-01) y recuperar contraseña (US-06).
  {
    element: <PublicLayout />,
    children: [
      {
        element: <RedirectIfAuthenticated />,
        children: [
          { path: PATHS.login, element: <LoginPage /> },
          { path: PATHS.registro, element: <RegistroPage /> },
        ],
      },
      { path: PATHS.ping, element: <PingPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
  // Pantallas con sesión pero estilo público / registro (wireframes "Datos de Empresa" y "Datos de Monotributista"):
  // No llevan menú lateral oscuro ni "Inicio", tienen fondo gris y logo arriba a la izquierda.
  {
    element: <RequireAuth />,
    children: [
      {
        element: <PublicLayout />,
        children: [
          { path: PATHS.solicitarPerfil, element: <SolicitarPerfilPage /> },
          { path: PATHS.estadoSolicitud, element: <EstadoSolicitudPage /> },
        ],
      },
    ],
  },
  // Pantallas autenticadas con menú lateral completo (AppLayout)
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: PATHS.inicio, element: <InicioPage /> },
        ],
      },
    ],
  },
]);
