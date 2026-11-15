import { useMutation, useQuery } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';
import type { ApiSchema } from '../../../shared/api/types';

type SolicitarRecuperoRequest = ApiSchema<'SolicitarRecuperoRequest'>;
type RestablecerPasswordRequest = ApiSchema<'RestablecerPasswordRequest'>;

/** US-06: pide el enlace. El backend responde igual exista o no el email (CA-2). */
export function useSolicitarRecupero() {
  return useMutation({
    mutationFn: async (body: SolicitarRecuperoRequest) => {
      await httpClient.post('/auth/recupero', body);
    },
  });
}

/**
 * US-06: al abrir el enlace del correo, si todavía sirve (CA-4). No lo consume. Sin código en la dirección no se
 * pregunta: la pantalla muestra el error directamente.
 */
export function useValidarRecupero(token: string | null) {
  return useQuery({
    queryKey: ['recupero', 'validar', token],
    queryFn: async () => {
      await httpClient.post('/auth/recupero/validar', { token });
      return true;
    },
    enabled: Boolean(token),
    retry: false,
    // Se pregunta cada vez que se abre: el enlace puede vencer o usarse en otra pestaña
    staleTime: 0,
    gcTime: 0,
  });
}

/** US-06: define la contraseña nueva y consume el enlace (CA-3). */
export function useRestablecerPassword() {
  return useMutation({
    mutationFn: async (body: RestablecerPasswordRequest) => {
      await httpClient.post('/auth/recupero/restablecer', body);
    },
  });
}
