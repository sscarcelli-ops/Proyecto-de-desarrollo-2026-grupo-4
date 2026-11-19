import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';
import type { ApiSchema } from '../../../shared/api/types';
import type { PageResponse } from '../../../shared/types/page';

// Tipos generados desde los DTO del backend (npm run api:types)
export type PerfilBarrio = ApiSchema<'PerfilBarrioResponse'>;

export type CondicionBarrio = 'HABILITADO' | 'DESHABILITADO';

// US-35: filtros del listado. Sin valor = todos.
export interface FiltrosPerfilesBarrio {
  tipo?: PerfilBarrio['tipo'];
  condicion?: CondicionBarrio;
}

export const habilitacionesKeys = {
  all: ['barrio', 'perfiles'] as const,
  page: (page: number, size: number, filtros: FiltrosPerfilesBarrio) =>
    ['barrio', 'perfiles', page, size, filtros.tipo ?? null, filtros.condicion ?? null] as const,
};

// US-33: Empresas y Monotributistas aprobados con su habilitación en el barrio del Operador.
// page empieza en 1 en la pantalla y en 0 en el backend.
async function getPerfilesDelBarrio(
  page: number,
  size: number,
  filtros: FiltrosPerfilesBarrio,
): Promise<PageResponse<PerfilBarrio>> {
  const { data } = await httpClient.get<PageResponse<PerfilBarrio>>('/barrio/perfiles', {
    params: { page: page - 1, size, tipo: filtros.tipo, condicion: filtros.condicion },
  });
  return data;
}

export function usePerfilesDelBarrio(page: number, size: number, filtros: FiltrosPerfilesBarrio = {}) {
  return useQuery({
    queryKey: habilitacionesKeys.page(page, size, filtros),
    queryFn: () => getPerfilesDelBarrio(page, size, filtros),
    placeholderData: keepPreviousData,
  });
}

// US-34: habilitar o deshabilitar un perfil en el barrio del Operador
async function cambiarHabilitacion({ id, habilitar }: { id: number; habilitar: boolean }): Promise<PerfilBarrio> {
  const accion = habilitar ? 'habilitar' : 'deshabilitar';
  const { data } = await httpClient.post<PerfilBarrio>(`/barrio/perfiles/${id}/${accion}`);
  return data;
}

export function useCambiarHabilitacion() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: cambiarHabilitacion,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: habilitacionesKeys.all }),
  });
}
