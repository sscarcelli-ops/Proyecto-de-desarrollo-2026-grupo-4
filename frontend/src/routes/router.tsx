import { createBrowserRouter } from 'react-router';
import { PATHS } from './paths';
import { NotFoundPage } from './NotFoundPage';
import { RedirectIfAuthenticated, RequireAuth } from './guards';
import { AppLayout } from '../app/layout/AppLayout';
import { PublicLayout } from '../app/layout/PublicLayout';
import { LoginPage } from '../features/auth/pages/LoginPage';
import { InicioPage } from '../features/auth/pages/InicioPage';
import { PingPage } from '../features/health/pages/PingPage';
import { BarriosPage } from '../features/barrios/pages/BarriosPage';
import { BarrioNuevoPage } from '../features/barrios/pages/BarrioNuevoPage';

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
  // Pantallas con sesión: menú lateral según el rol. En US-04 se envuelve con el guard que
  // redirige al login si no hay usuario; cada feature agrega acá sus páginas y su opción en menuItems.
  {
    element: <AppLayout />,
    children: [],
  },
]);
