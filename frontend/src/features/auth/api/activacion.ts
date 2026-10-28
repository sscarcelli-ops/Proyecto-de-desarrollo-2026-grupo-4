import { useMutation } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';
import type { ApiSchema } from '../../../shared/api/types';

type ValidarActivacionRequest = ApiSchema<'ValidarActivacionRequest'>;
export type ActivacionRequest = ApiSchema<'ActivacionRequest'>;

/** US-01, "Activación de cuenta": comprueba que el código sirve. No lo consume. */
export function useValidarActivacion() {
  return useMutation({
    mutationFn: async (body: ValidarActivacionRequest) => {
      await httpClient.post('/auth/activacion/validar', body);
    },
  });
}

/** US-01, "Registrate": crea el Administrador y consume el código. No inicia sesión: después va al login. */
export function useActivar() {
  return useMutation({
    mutationFn: async (body: ActivacionRequest) => {
      await httpClient.post('/auth/activacion', body);
    },
  });
}
