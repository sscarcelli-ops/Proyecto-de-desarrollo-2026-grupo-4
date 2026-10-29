import { createBrowserRouter } from 'react-router';
import { PATHS } from './paths';
import { NotFoundPage } from './NotFoundPage';
import { RedirectIfAuthenticated, RequireAuth } from './guards';
import { AppLayout } from '../app/layout/AppLayout';
import { PublicLayout } from '../app/layout/PublicLayout';
import { LoginPage } from '../features/auth/pages/LoginPage';
import { InicioPage } from '../features/auth/pages/InicioPage';
import { PingPage } from '../features/health/pages/PingPage';
import { GaritaPage } from '../features/garita/pages/GaritaPage';

export const router = createBrowserRouter([
  // Pantallas sin sesión: login (US-04). Se suman activación del Administrador (US-01), registro (US-02)
  // y recuperar contraseña (US-06).
  {
    element: <PublicLayout />,
    children: [
      {
        element: <RedirectIfAuthenticated />,
        children: [{ path: PATHS.login, element: <LoginPage /> }],
      },
      { path: PATHS.ping, element: <PingPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
  // Pantallas con sesión:
  {
    element: <RequireAuth />,
    children: [
      // Menú lateral para roles de administración / oficina
      {
        element: <AppLayout />,
        children: [{ path: PATHS.inicio, element: <InicioPage /> }],
      },
      // Garita (Guardia): pantalla móvil dedicada sin sidebar (wireframe RNF-05)
      { path: PATHS.garita, element: <GaritaPage /> },
    ],
  },
]);
