import { useMutation, useQueryClient } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';
import { authToken } from '../../../shared/lib/authToken';
import type { ApiSchema } from '../../../shared/api/types';
import { CURRENT_USER_KEY, toCurrentUser } from './useCurrentUser';
import { MI_SOLICITUD_KEY } from '../../perfiles/api/perfiles';

type LoginRequest = ApiSchema<'LoginRequest'>;
type LoginResponse = ApiSchema<'LoginResponse'>;
type RegistroRequest = ApiSchema<'RegistroRequest'>;

/** US-04: inicia sesión, guarda el token y deja el usuario en la caché (sin otra consulta a /me). */
export function useLogin() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: LoginRequest) => (await httpClient.post<LoginResponse>('/auth/login', body)).data,
    onSuccess: (data) => {
      authToken.set(data.token);
      queryClient.setQueryData(CURRENT_USER_KEY, toCurrentUser(data));
      // Invalida la consulta de solicitud por si inició sesión otra cuenta
      queryClient.invalidateQueries({ queryKey: MI_SOLICITUD_KEY });
    },
  });
}

/** US-02: crea cuenta base, guarda el token y autentica automáticamente para continuar a US-03. */
export function useRegistro() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: RegistroRequest) => (await httpClient.post<LoginResponse>('/auth/registro', body)).data,
    onSuccess: (data) => {
      authToken.set(data.token);
      queryClient.setQueryData(CURRENT_USER_KEY, toCurrentUser(data));
      // Invalida la query de solicitud para que las páginas con sesión reconozcan que no tiene perfil aún
      queryClient.invalidateQueries({ queryKey: MI_SOLICITUD_KEY });
    },
  });
}

/** US-05 / RF-25: cierra la sesión. El token vive en localStorage: siempre se borra, junto con el usuario en caché. */
export function useLogout() {
  const queryClient = useQueryClient();
  return () => {
    authToken.clear();
    sessionStorage.removeItem('registro_tipo');
    queryClient.removeQueries({ queryKey: CURRENT_USER_KEY });
    queryClient.removeQueries({ queryKey: MI_SOLICITUD_KEY });
  };
}
