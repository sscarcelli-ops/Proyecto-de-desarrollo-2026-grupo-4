import type { Rol } from '../shared/types/rol';
import { PATHS } from './paths';

/**
 * Pantalla a la que va cada usuario al iniciar sesión, según su rol (US-04: redirección por rol).
 * Hoy todos van a Inicio. Cada US que crea la pantalla principal de un rol la conecta acá con una línea, por ej.
 * la garita (US-37): if (roles.includes('GUARDIA')) return PATHS.garita;
 * Sin rol aprobado (roles vacíos), el estado de la solicitud (US-08).
 */
export function pantallaInicial(roles: Rol[]): string {
  void roles;
  return PATHS.inicio;
}
