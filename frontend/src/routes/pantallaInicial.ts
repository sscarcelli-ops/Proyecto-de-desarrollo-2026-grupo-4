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
 * - Con rol aprobado -> pantalla principal de su rol (hoy /inicio para todos).
 */
export function pantallaInicial(roles: Rol[], perfil?: PerfilInfo | null): string {
  if (roles.length === 0) {
    if (perfil) {
      return PATHS.estadoSolicitud;
    }
    return PATHS.solicitarPerfil;
  }
  return PATHS.inicio;
}
