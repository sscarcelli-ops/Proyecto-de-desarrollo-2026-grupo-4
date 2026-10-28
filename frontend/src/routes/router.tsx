import { createBrowserRouter } from 'react-router';
import { PATHS } from './paths';
import { NotFoundPage } from './NotFoundPage';
import { RedirectIfAuthenticated, RequireAuth } from './guards';
import { AppLayout } from '../app/layout/AppLayout';
import { PublicLayout } from '../app/layout/PublicLayout';
import { LoginPage } from '../features/auth/pages/LoginPage';
import { ActivacionPage } from '../features/auth/pages/ActivacionPage';
import { InicioPage } from '../features/auth/pages/InicioPage';
import { PingPage } from '../features/health/pages/PingPage';

export const router = createBrowserRouter([
  // Pantallas sin sesión: login (US-04) y activación del Administrador (US-01). Se suman registro (US-02)
  // y recuperar contraseña (US-06).
  {
    element: <PublicLayout />,
    children: [
      {
        element: <RedirectIfAuthenticated />,
        children: [
          { path: PATHS.login, element: <LoginPage /> },
          { path: PATHS.activar, element: <ActivacionPage /> },
        ],
      },
      { path: PATHS.ping, element: <PingPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
  // Pantallas con sesión: menú lateral según el rol. Cada feature agrega acá sus páginas y su opción en menuItems.
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppLayout />,
        children: [{ path: PATHS.inicio, element: <InicioPage /> }],
      },
    ],
  },
]);
