import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { FormInstance } from 'antd';
import { httpClient } from '../../../shared/lib/httpClient';
import { getApiProblem, getErrorMessage } from '../../../shared/lib/apiError';
import type { ApiSchema } from '../../../shared/api/types';

// Tipos generados desde los DTO del backend (npm run api:types)
export type Operador = ApiSchema<'OperadorResponse'>;
export type OperadorRequest = ApiSchema<'OperadorRequest'>;

export const operadoresKeys = {
  all: ['operadores'] as const,
  disponibles: ['operadores', 'disponibles'] as const,
};

// US-13: crea el Operador. Queda "Disponible" (sin barrio) hasta que se lo asigne (US-45).
async function crearOperador(request: OperadorRequest): Promise<Operador> {
  const { data } = await httpClient.post<Operador>('/operadores', request);
  return data;
}

export function useCrearOperador() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: crearOperador,
    // Para que los listados de Operadores (US-15) y de disponibles (US-45) vuelvan a pedirse
    onSuccess: () => queryClient.invalidateQueries({ queryKey: operadoresKeys.all }),
  });
}

// US-45: Operadores sin barrio, para elegir a quién asignar
async function getOperadoresDisponibles(): Promise<Operador[]> {
  const { data } = await httpClient.get<Operador[]>('/operadores/disponibles');
  return data;
}

export function useOperadoresDisponibles() {
  return useQuery({
    queryKey: operadoresKeys.disponibles,
    queryFn: getOperadoresDisponibles,
  });
}

/**
 * Marca en el formulario los errores que devuelve el backend: el email repetido va debajo del campo email
 * y el resto de las validaciones en su propio campo (también cuando el Operador viaja dentro de otra petición,
 * como en US-45, donde el backend los nombra "nuevoOperador.nombre"). Devuelve true si pudo marcar algo en
 * el formulario; si no, el que llama muestra el mensaje general (getErrorMessage).
 */
export function marcarErroresDeOperador(form: FormInstance, error: unknown): boolean {
  const problem = getApiProblem(error);
  if (problem?.code === 'EMAIL_DUPLICADO') {
    form.setFields([{ name: 'email', errors: [getErrorMessage(error)] }]);
    return true;
  }
  const fieldErrors = (problem?.errors ?? []).map(({ field, message }) => ({
    name: field.replace(/^nuevoOperador\./, ''),
    errors: [message],
  }));
  if (fieldErrors.length > 0) {
    form.setFields(fieldErrors);
    return true;
  }
  return false;
}
