import { createBrowserRouter } from 'react-router';
import { PATHS } from './paths';
import { NotFoundPage } from './NotFoundPage';
import { AppLayout } from '../app/layout/AppLayout';
import { PublicLayout } from '../app/layout/PublicLayout';
import { PingPage } from '../features/health/pages/PingPage';
import { BarriosPage } from '../features/barrios/pages/BarriosPage';
import { BarrioNuevoPage } from '../features/barrios/pages/BarrioNuevoPage';

export const router = createBrowserRouter([
  // Pantallas sin sesión: login (US-04, será la ruta "/"), activación del Administrador (US-01),
  // registro (US-02), recuperar contraseña (US-05). Por ahora, la prueba de conexión de la T-02.
  {
    element: <PublicLayout />,
    children: [
      { path: PATHS.home, element: <PingPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
  // Pantallas con sesión: menú lateral según el rol. En US-04 se envuelve con el guard que
  // redirige al login si no hay usuario; cada feature agrega acá sus páginas y su opción en menuItems.
  {
    element: <AppLayout />,
    children: [
      { path: PATHS.barrios, element: <BarriosPage /> },
      { path: PATHS.barrioNuevo, element: <BarrioNuevoPage /> },
    ],
  },
]);
