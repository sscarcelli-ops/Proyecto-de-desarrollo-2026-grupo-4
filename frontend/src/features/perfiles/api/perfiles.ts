import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';
import type { ApiSchema } from '../../../shared/api/types';

export type PerfilResponse = ApiSchema<'PerfilResponse'>;

export const MI_SOLICITUD_KEY = ['perfil', 'mi-solicitud'];

export function useMiSolicitud() {
  return useQuery({
    queryKey: MI_SOLICITUD_KEY,
    queryFn: async () => {
      try {
        const res = await httpClient.get<PerfilResponse>('/perfil/mi-solicitud');
        return res.data;
      } catch (err: unknown) {
        // Si no tiene perfil aún (404), devuelve null en vez de romper la query
        const e = err as { response?: { status?: number } };
        if (e.response?.status === 404) {
          return null;
        }
        throw err;
      }
    },
    retry: false,
  });
}

export function useSolicitarPerfil() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (formData: FormData) => {
      const res = await httpClient.post<PerfilResponse>('/perfil', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      return res.data;
    },
    onSuccess: (data) => {
      queryClient.setQueryData(MI_SOLICITUD_KEY, data);
    },
  });
}
