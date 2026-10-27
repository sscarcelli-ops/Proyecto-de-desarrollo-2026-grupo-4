import { useMutation, useQueryClient } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';
import { authToken } from '../../../shared/lib/authToken';
import type { ApiSchema } from '../../../shared/api/types';
import { CURRENT_USER_KEY, toCurrentUser } from './useCurrentUser';

type LoginRequest = ApiSchema<'LoginRequest'>;
type LoginResponse = ApiSchema<'LoginResponse'>;

/** US-04: inicia sesión, guarda el token y deja el usuario en la caché (sin otra consulta a /me). */
export function useLogin() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: LoginRequest) => (await httpClient.post<LoginResponse>('/auth/login', body)).data,
    onSuccess: (data) => {
      authToken.set(data.token);
      queryClient.setQueryData(CURRENT_USER_KEY, toCurrentUser(data));
    },
  });
}

/** US-05 / RF-25: cierra la sesión. El token vive en localStorage: siempre se borra, junto con el usuario en caché. */
export function useLogout() {
  const queryClient = useQueryClient();
  return () => {
    authToken.clear();
    queryClient.removeQueries({ queryKey: CURRENT_USER_KEY });
  };
}
