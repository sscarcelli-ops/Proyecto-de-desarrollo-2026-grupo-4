import { useQuery } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';

export interface MovimientoHistorial {
  id: number;
  fechaHora: string;
  tipo: 'INGRESO' | 'EGRESO';
  trabajadorNombre: string;
  trabajadorApellido: string;
  trabajadorDni: string;
  entidadTipo: 'EMPRESA' | 'MONOTRIBUTISTA';
  entidadNombre: string;
  motivo?: string | null;
}

export interface EntidadFiltro {
  id: number;
  nombre: string;
  tipo: 'EMPRESA' | 'MONOTRIBUTISTA';
}

export interface PaginaMovimientos {
  content: MovimientoHistorial[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FiltrosHistorial {
  desde?: string;
  hasta?: string;
  dni?: string;
  perfilId?: number;
}

export function useHistorialAccesos(pagina: number, tamano: number, filtros: FiltrosHistorial) {
  return useQuery({
    queryKey: ['historial-accesos', pagina, tamano, filtros],
    queryFn: async () => {
      const params = new URLSearchParams();
      params.set('page', String(pagina - 1));
      params.set('size', String(tamano));
      if (filtros.desde) params.set('desde', filtros.desde);
      if (filtros.hasta) params.set('hasta', filtros.hasta);
      if (filtros.dni) params.set('dni', filtros.dni);
      if (filtros.perfilId) params.set('perfilId', String(filtros.perfilId));

      const { data } = await httpClient.get<PaginaMovimientos>(`/barrio/movimientos?${params.toString()}`);
      return data;
    },
  });
}

export function useEntidadesFiltro() {
  return useQuery({
    queryKey: ['entidades-filtro-barrio'],
    queryFn: async () => {
      const { data } = await httpClient.get<EntidadFiltro[]>('/barrio/movimientos/entidades');
      return data;
    },
  });
}
