import axios from 'axios';
import { useMutation, useQuery } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';
import type { ApiSchema } from '../../../shared/api/types';

type ValidarActivacionRequest = ApiSchema<'ValidarActivacionRequest'>;
export type ActivacionRequest = ApiSchema<'ActivacionRequest'>;

/**
 * US-01: si la activación sigue abierta (todavía no hay Administrador). Con el Administrador creado el backend
 * responde 404 y la pantalla muestra "página no encontrada", sin ningún campo para escribir.
 */
export function useActivacionDisponible() {
  return useQuery({
    queryKey: ['activacion', 'disponible'],
    queryFn: async () => {
      try {
        await httpClient.get('/auth/activacion');
        return true;
      } catch (error) {
        if (axios.isAxiosError(error) && error.response?.status === 404) return false;
        throw error;
      }
    },
    // Se pregunta cada vez que se abre la pantalla: puede cerrarse en cualquier momento
    staleTime: 0,
    gcTime: 0,
  });
}

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
