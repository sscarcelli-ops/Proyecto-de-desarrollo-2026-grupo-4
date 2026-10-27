import type { Rol } from '../shared/types/rol';
import { PATHS } from './paths';

/**
 * Pantalla a la que va cada usuario al iniciar sesión, según su rol (US-04: redirección por rol).
 * - EMPRESA: va directamente a la gestión de sus trabajadores (US-26).
 * - MONOTRIBUTISTA: su propio trabajador / datos personales.
 */
export function pantallaInicial(roles: Rol[]): string {
  if (roles.includes('EMPRESA')) {
    return PATHS.trabajadores;
  }
  if (roles.includes('MONOTRIBUTISTA')) {
    return PATHS.trabajadores; // o su pantalla de datos personales cuando esté disponible
  }
  return PATHS.inicio;
}
