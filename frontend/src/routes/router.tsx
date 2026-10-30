import { createBrowserRouter } from 'react-router';
import { PATHS } from './paths';
import { NotFoundPage } from './NotFoundPage';
import { RedirectIfAuthenticated, RequireAuth, RequireRole } from './guards';
import { AppLayout } from '../app/layout/AppLayout';
import { PublicLayout } from '../app/layout/PublicLayout';
import { LoginPage } from '../features/auth/pages/LoginPage';
import { RegistroPage } from '../features/auth/pages/RegistroPage';
import { InicioPage } from '../features/auth/pages/InicioPage';
import { SolicitarPerfilPage } from '../features/perfiles/pages/SolicitarPerfilPage';
import { EstadoSolicitudPage } from '../features/perfiles/pages/EstadoSolicitudPage';
import { PingPage } from '../features/health/pages/PingPage';
import { GaritaPage } from '../features/garita/pages/GaritaPage';
import { BarriosPage } from '../features/barrios/pages/BarriosPage';
import { BarrioNuevoPage } from '../features/barrios/pages/BarrioNuevoPage';
import { BarrioEditarPage } from '../features/barrios/pages/BarrioEditarPage';
import { TrabajadoresPage } from '../features/trabajadores/pages/TrabajadoresPage';
import { TrabajadorNuevoPage } from '../features/trabajadores/pages/TrabajadorNuevoPage';
import { TrabajadorEditarPage } from '../features/trabajadores/pages/TrabajadorEditarPage';

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
      // Menú lateral para roles de administración / oficina
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
      // Garita (Guardia): pantalla móvil dedicada sin sidebar (wireframe RNF-05)
      {
        element: <RequireRole roles={['GUARDIA']} />,
        children: [{ path: PATHS.garita, element: <GaritaPage /> }],
      },
    ],
  },
]);
