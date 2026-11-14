import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { FormInstance } from 'antd';
import { httpClient } from '../../../shared/lib/httpClient';
import { getApiProblem, getErrorMessage, toFormFieldErrors } from '../../../shared/lib/apiError';
import type { ApiSchema } from '../../../shared/api/types';

// Tipos generados desde los DTO del backend (npm run api:types, con el backend corriendo)
export type Guardia = ApiSchema<'GuardiaResponse'>;
export type GuardiaRequest = ApiSchema<'GuardiaRequest'>;
export type GuardiaModificarRequest = ApiSchema<'GuardiaModificarRequest'>;

export const guardiasKeys = {
  all: ['guardias'] as const,
  detalle: (id: string) => ['guardias', id] as const,
};

// US-32: Guardias del barrio del Operador. Sin paginar (bajo volumen por barrio, RNF-08).
async function getGuardias(): Promise<Guardia[]> {
  const { data } = await httpClient.get<Guardia[]>('/guardias');
  return data;
}

export function useGuardias() {
  return useQuery({ queryKey: guardiasKeys.all, queryFn: getGuardias });
}

export function useGuardia(id: string | undefined) {
  return useQuery({
    queryKey: guardiasKeys.detalle(id ?? ''),
    queryFn: async () => (await httpClient.get<Guardia>(`/guardias/${id}`)).data,
    enabled: id !== undefined,
  });
}

export function useCrearGuardia() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: GuardiaRequest) => (await httpClient.post<Guardia>('/guardias', body)).data,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: guardiasKeys.all }),
  });
}

export function useModificarGuardia(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: GuardiaModificarRequest) => (await httpClient.put<Guardia>(`/guardias/${id}`, body)).data,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: guardiasKeys.all }),
  });
}

// Baja lógica y reversible (RN-09): habilitado = false deshabilita, true vuelve a habilitar
export function useCambiarEstadoGuardia() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, habilitado }: { id: number; habilitado: boolean }) =>
      (await httpClient.put<Guardia>(`/guardias/${id}/estado`, { habilitado })).data,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: guardiasKeys.all }),
  });
}

/**
 * Marca en el formulario los errores del backend: el email repetido debajo del campo email, la contraseña que no
 * cumple la política debajo de la contraseña y el resto de las validaciones en su propio campo. Devuelve true si
 * pudo marcar algo; si no, el que llama muestra el mensaje general (getErrorMessage).
 */
export function marcarErroresDeGuardia(form: FormInstance, error: unknown): boolean {
  const problem = getApiProblem(error);
  if (problem?.code === 'EMAIL_DUPLICADO') {
    form.setFields([{ name: 'email', errors: [getErrorMessage(error)] }]);
    return true;
  }
  if (problem?.code === 'PASSWORD_INVALIDA') {
    form.setFields([{ name: 'password', errors: [getErrorMessage(error)] }]);
    return true;
  }
  const errores = toFormFieldErrors(error);
  if (errores.length > 0) {
    form.setFields(errores);
    return true;
  }
  return false;
}
