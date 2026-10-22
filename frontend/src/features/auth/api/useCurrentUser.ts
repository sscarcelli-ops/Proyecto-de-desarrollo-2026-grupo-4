import type { Rol } from '../../../shared/types/rol';

export interface CurrentUser {
  email: string;
  roles: Rol[];
}

/**
 * Usuario autenticado, o null si no hay sesión. Es el único punto del que el layout, el menú
 * y los guards toman los roles.
 * US-04: reemplazar por la consulta real (ej. GET /api/auth/me con TanStack Query, usando el token guardado).
 */
export function useCurrentUser(): CurrentUser | null {
  return null;
}
