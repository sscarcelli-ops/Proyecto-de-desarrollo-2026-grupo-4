import { useQuery } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';
import { authToken } from '../../../shared/lib/authToken';
import type { ApiSchema } from '../../../shared/api/types';
import type { Rol } from '../../../shared/types/rol';

export interface CurrentUser {
  email: string;
  // "Carlos Gómez", la razón social de una Empresa, etc. null para el Administrador o una cuenta sin perfil
  nombre: string | null;
  roles: Rol[];
}

// Clave del usuario de la sesión en la caché de TanStack Query (la completa el login y la borra el logout)
export const CURRENT_USER_KEY = ['auth', 'me'] as const;

export function toCurrentUser(data: ApiSchema<'UsuarioActualResponse'> | ApiSchema<'LoginResponse'>): CurrentUser {
  return { email: data.email, nombre: data.nombre, roles: data.roles as Rol[] };
}

async function getMe(): Promise<CurrentUser> {
  const { data } = await httpClient.get<ApiSchema<'UsuarioActualResponse'>>('/auth/me');
  return toCurrentUser(data);
}

/** Consulta del usuario de la sesión (GET /api/auth/me). Solo se hace si hay un token guardado. */
export function useCurrentUserQuery() {
  return useQuery({
    queryKey: CURRENT_USER_KEY,
    queryFn: getMe,
    enabled: authToken.get() !== null,
    staleTime: Infinity,
    retry: false,
  });
}

/**
 * Usuario autenticado, o null si no hay sesión. Es el único punto del que el layout, el menú
 * y los guards toman los roles.
 */
export function useCurrentUser(): CurrentUser | null {
  return useCurrentUserQuery().data ?? null;
}
