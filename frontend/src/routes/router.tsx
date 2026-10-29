import { createBrowserRouter } from 'react-router';
import { PATHS } from './paths';
import { NotFoundPage } from './NotFoundPage';
import { RedirectIfAuthenticated, RequireAuth, RequireRole } from './guards';
import { AppLayout } from '../app/layout/AppLayout';
import { PublicLayout } from '../app/layout/PublicLayout';
import { LoginPage } from '../features/auth/pages/LoginPage';
import { InicioPage } from '../features/auth/pages/InicioPage';
import { PingPage } from '../features/health/pages/PingPage';
import { BarriosPage } from '../features/barrios/pages/BarriosPage';
import { BarrioNuevoPage } from '../features/barrios/pages/BarrioNuevoPage';
import { BarrioEditarPage } from '../features/barrios/pages/BarrioEditarPage';
import { TrabajadoresPage } from '../features/trabajadores/pages/TrabajadoresPage';
import { TrabajadorNuevoPage } from '../features/trabajadores/pages/TrabajadorNuevoPage';
import { TrabajadorEditarPage } from '../features/trabajadores/pages/TrabajadorEditarPage';

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
  // Pantallas con sesión: menú lateral según el rol. Cada feature agrega acá sus páginas (bajo RequireRole)
  // y su opción en menuItems.
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: PATHS.inicio, element: <InicioPage /> },
          // Barrios: exclusivo del Administrador (US-10, US-11)
          {
            element: <RequireRole roles={['ADMIN']} />,
            children: [
              { path: PATHS.barrios, element: <BarriosPage /> },
              { path: PATHS.barrioNuevo, element: <BarrioNuevoPage /> },
              { path: PATHS.barrioEditar, element: <BarrioEditarPage /> },
            ],
          },
          // Trabajadores: exclusivo para rol EMPRESA (US-25, US-26)
          {
            element: <RequireRole roles={['EMPRESA']} />,
            children: [
              { path: PATHS.trabajadores, element: <TrabajadoresPage /> },
              { path: PATHS.trabajadorNuevo, element: <TrabajadorNuevoPage /> },
              { path: PATHS.trabajadorEditar, element: <TrabajadorEditarPage /> },
            ],
          },
        ],
      },
    ],
  },
]);
