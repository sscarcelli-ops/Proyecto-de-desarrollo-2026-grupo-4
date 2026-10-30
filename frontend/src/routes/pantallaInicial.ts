import type { Rol } from '../shared/types/rol';
import { PATHS } from './paths';

export interface PerfilInfo {
  estado?: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'SUSPENDIDO';
}

/**
 * Pantalla a la que va cada usuario al iniciar sesión, según su rol y estado de solicitud.
 * - Sin rol:
 *   - Si tiene solicitud enviada (pendiente/rechazada) -> /estado-solicitud (US-08)
 *   - Si no completó datos de perfil -> /solicitar-perfil (US-03 CA-5)
 * - Con rol aprobado:
 *   - GUARDIA: va directamente al control de acceso de Garita (US-37 a 40).
 *   - EMPRESA: va directamente a la gestión de sus trabajadores (US-26).
 *   - Otros roles: van a su pantalla correspondiente o Inicio.
 */
export function pantallaInicial(roles: Rol[], perfil?: PerfilInfo | null): string {
  if (roles.length === 0) {
    if (perfil) {
      return PATHS.estadoSolicitud;
    }
    return PATHS.solicitarPerfil;
  }
  if (roles.includes('GUARDIA')) {
    return PATHS.garita;
  }
  if (roles.includes('EMPRESA')) {
    return PATHS.trabajadores;
  }
  // Monotributista irá a su pantalla de datos personales cuando se implemente en S2
  return PATHS.inicio;
}
