import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';
import type { ApiSchema } from '../../../shared/api/types';

// Tipos generados desde los DTO del backend (npm run api:types)
export type Barrio = ApiSchema<'BarrioResponse'>;
export type BarrioRequest = ApiSchema<'BarrioRequest'>;

export const barriosKeys = {
  all: ['barrios'] as const,
  detail: (id: string) => ['barrios', id] as const,
};

// US-10: el listado de barrios no se pagina (devuelve un array)
async function getBarrios(): Promise<Barrio[]> {
  const { data } = await httpClient.get<Barrio[]>('/barrios');
  return data;
}

export function useBarrios() {
  return useQuery({
    queryKey: barriosKeys.all,
    queryFn: getBarrios,
  });
}

// US-11: un barrio, para precargar el formulario de edición
async function getBarrio(id: string): Promise<Barrio> {
  const { data } = await httpClient.get<Barrio>(`/barrios/${id}`);
  return data;
}

export function useBarrio(id: string | undefined) {
  return useQuery({
    queryKey: barriosKeys.detail(id ?? ''),
    queryFn: () => getBarrio(id as string),
    enabled: id !== undefined,
    retry: false, // un 404 no mejora reintentando
  });
}

// US-10: alta de barrio. El barrio nace Inactivo y sin Operador.
async function crearBarrio(request: BarrioRequest): Promise<Barrio> {
  const { data } = await httpClient.post<Barrio>('/barrios', request);
  return data;
}

export function useCrearBarrio() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: crearBarrio,
    // El listado se vuelve a pedir para que figure el barrio nuevo
    onSuccess: () => queryClient.invalidateQueries({ queryKey: barriosKeys.all }),
  });
}

// US-11: modifica nombre, localidad/provincia y dirección (no el estado ni el Operador)
async function modificarBarrio(id: string, request: BarrioRequest): Promise<Barrio> {
  const { data } = await httpClient.put<Barrio>(`/barrios/${id}`, request);
  return data;
}

export function useModificarBarrio(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: BarrioRequest) => modificarBarrio(id, request),
    // barriosKeys.all es prefijo de detail: refresca el listado y el detalle
    onSuccess: () => queryClient.invalidateQueries({ queryKey: barriosKeys.all }),
  });
}
