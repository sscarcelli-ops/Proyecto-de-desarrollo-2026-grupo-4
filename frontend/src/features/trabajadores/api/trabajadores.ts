import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';
import type { ApiSchema } from '../../../shared/api/types';
import type { PageResponse } from '../../../shared/types/page';

// ========================================================================================
// Tipos — generados desde los DTOs del backend (npm run api:types)
// ========================================================================================

export type TrabajadorResponse = ApiSchema<'TrabajadorResponse'>;
export type TrabajadorRequest = ApiSchema<'TrabajadorRequest'>;
export type TrabajadorEditarRequest = ApiSchema<'TrabajadorEditarRequest'>;

// ========================================================================================
// Claves de caché de TanStack Query
// ========================================================================================

// Todas las queries de trabajadores comparten esta raíz. Cuando se crea o edita un
// trabajador, se invalidan todas las queries que empiecen con esta clave, así la tabla
// se refresca automáticamente sin recargar la página.
const TRABAJADORES_KEY = ['trabajadores'] as const;

// ========================================================================================
// Funciones de fetch (llamadas HTTP puras)
// ========================================================================================

/** GET /api/trabajadores?page=X&size=Y&sort=Z — listado paginado */
async function listarTrabajadores(page: number, size: number): Promise<PageResponse<TrabajadorResponse>> {
  // Ant Design numera las páginas desde 1, pero Spring las numera desde 0.
  // La conversión se hace acá para que las páginas no tengan que preocuparse por eso.
  const { data } = await httpClient.get<PageResponse<TrabajadorResponse>>('/trabajadores', {
    params: { page: page - 1, size, sort: 'nombre,asc' },
  });
  return data;
}

/** GET /api/trabajadores/:id — detalle de un trabajador */
async function obtenerTrabajador(id: number): Promise<TrabajadorResponse> {
  const { data } = await httpClient.get<TrabajadorResponse>(`/trabajadores/${id}`);
  return data;
}

/** POST /api/trabajadores — registrar un nuevo trabajador (US-25) */
async function crearTrabajador(body: TrabajadorRequest): Promise<TrabajadorResponse> {
  const { data } = await httpClient.post<TrabajadorResponse>('/trabajadores', body);
  return data;
}

/** PUT /api/trabajadores/:id — editar nombre y apellido (US-26) */
async function editarTrabajador(id: number, body: TrabajadorEditarRequest): Promise<TrabajadorResponse> {
  const { data } = await httpClient.put<TrabajadorResponse>(`/trabajadores/${id}`, body);
  return data;
}

// ========================================================================================
// Hooks de TanStack Query — los que usan las páginas
// ========================================================================================

/**
 * Listado paginado de trabajadores (US-26).
 * La tabla le pasa la página y el tamaño actual; el hook hace el fetch y cachea el resultado.
 * Cuando cambia la página, TanStack Query hace un nuevo fetch automáticamente (porque cambia la queryKey).
 *
 * keepPreviousData: mientras carga la página nueva, sigue mostrando la anterior (evita que
 * la tabla parpadee vacía al cambiar de página).
 */
export function useTrabajadores(page: number, size: number = 10) {
  return useQuery({
    queryKey: [...TRABAJADORES_KEY, { page, size }],
    queryFn: () => listarTrabajadores(page, size),
    placeholderData: (prev) => prev, // equivalente a keepPreviousData en v5
  });
}

/**
 * Detalle de un trabajador (para la página de edición).
 * Se activa solo si el id es válido (enabled: id > 0).
 */
export function useTrabajador(id: number) {
  return useQuery({
    queryKey: [...TRABAJADORES_KEY, id],
    queryFn: () => obtenerTrabajador(id),
    enabled: id > 0,
  });
}

/**
 * Registrar un nuevo trabajador (US-25).
 * onSuccess invalida la caché del listado para que la tabla se actualice sola.
 */
export function useCrearTrabajador() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: crearTrabajador,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: TRABAJADORES_KEY });
    },
  });
}

/**
 * Editar nombre y apellido de un trabajador (US-26).
 * onSuccess invalida la caché del listado y del detalle del trabajador editado.
 */
export function useEditarTrabajador(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: TrabajadorEditarRequest) => editarTrabajador(id, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: TRABAJADORES_KEY });
    },
  });
}
