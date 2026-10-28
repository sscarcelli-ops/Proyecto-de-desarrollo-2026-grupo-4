import type { Rol } from '../shared/types/rol';
import { PATHS } from './paths';

/**
 * Pantalla a la que va cada usuario al iniciar sesión, según su rol (US-04: redirección por rol).
 * - EMPRESA: va directamente a la gestión de sus trabajadores (US-26).
 * - Otros roles: van a su pantalla correspondiente o Inicio.
 */
export function pantallaInicial(roles: Rol[]): string {
  if (roles.includes('EMPRESA')) {
    return PATHS.trabajadores;
  }
  // Monotributista irá a su pantalla de datos personales cuando se implemente en S2
  return PATHS.inicio;
}
