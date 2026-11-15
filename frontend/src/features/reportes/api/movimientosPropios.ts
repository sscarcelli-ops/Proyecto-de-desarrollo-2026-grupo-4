import { httpClient } from '../../../shared/lib/httpClient';
import type { PageResponse } from '../../../shared/types/page';

export interface MovimientoPropio {
  id: number;
  fechaHora: string;
  tipo: 'INGRESO' | 'EGRESO';
  trabajadorNombre: string;
  trabajadorApellido: string;
  trabajadorDni: string;
  barrioNombre: string;
  motivo?: string | null;
}

export interface MovimientoPropioFiltros {
  desde?: string;
  hasta?: string;
  dni?: string;
  barrioId?: number;
  page?: number;
  size?: number;
}

export interface BarrioFiltro {
  id: number;
  nombre: string;
}

export async function getMovimientosPropios(filtros: MovimientoPropioFiltros = {}): Promise<PageResponse<MovimientoPropio>> {
  const { data } = await httpClient.get<PageResponse<MovimientoPropio>>('/propio/movimientos', {
    params: {
      desde: filtros.desde,
      hasta: filtros.hasta,
      dni: filtros.dni ? filtros.dni.trim() : undefined,
      barrioId: filtros.barrioId,
      page: filtros.page ?? 0,
      size: filtros.size ?? 10,
    },
  });
  return data;
}

export async function getBarriosHabilitados(): Promise<BarrioFiltro[]> {
  const { data } = await httpClient.get<BarrioFiltro[]>('/propio/movimientos/barrios');
  return data;
}
