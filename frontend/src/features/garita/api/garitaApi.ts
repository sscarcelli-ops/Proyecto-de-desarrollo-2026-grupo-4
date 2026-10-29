import { useMutation, useQueryClient } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';
import type { ApiSchema } from '../../../shared/api/types';

// Tipos oficiales generados desde Swagger (OpenAPI)
export type BusquedaDniResponse = ApiSchema<'BusquedaDniResponse'>;
export type VinculoResultado = ApiSchema<'VinculoResultado'>;
export type IngresoActivoInfo = ApiSchema<'IngresoActivoInfo'>;
export type VerificacionResponse = ApiSchema<'VerificacionResponse'>;
export type MovimientoResponse = ApiSchema<'MovimientoResponse'>;
export type VerificacionRequest = ApiSchema<'VerificacionRequest'>;
export type IngresoRequest = ApiSchema<'IngresoRequest'>;
export type EgresoRequest = ApiSchema<'EgresoRequest'>;

async function buscarPorDni(dni: string): Promise<BusquedaDniResponse> {
  const { data } = await httpClient.get<BusquedaDniResponse>('/garita/buscar', { params: { dni } });
  return data;
}

async function verificarAcceso(trabajadorId: number): Promise<VerificacionResponse> {
  const { data } = await httpClient.post<VerificacionResponse>('/garita/verificar', { trabajadorId });
  return data;
}

async function registrarIngreso(trabajadorId: number): Promise<MovimientoResponse> {
  const { data } = await httpClient.post<MovimientoResponse>('/garita/ingreso', { trabajadorId });
  return data;
}

async function registrarEgreso(trabajadorId: number): Promise<MovimientoResponse> {
  const { data } = await httpClient.post<MovimientoResponse>('/garita/egreso', { trabajadorId });
  return data;
}

/**
 * US-37: buscar trabajador por DNI. Se ejecuta como mutación al pulsar "Buscar Trabajador" (coincidencia exacta).
 */
export function useBuscarPorDni() {
  return useMutation({
    mutationFn: (dni: string) => buscarPorDni(dni),
  });
}

/** US-38: verificar habilitación del vínculo elegido. */
export function useVerificarAcceso() {
  return useMutation({ mutationFn: verificarAcceso });
}

/** US-39: registrar ingreso tras verificación positiva. */
export function useRegistrarIngreso() {
  return useMutation({ mutationFn: registrarIngreso });
}

/** US-40: registrar egreso de trabajador con ingreso activo. */
export function useRegistrarEgreso() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: registrarEgreso,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['garita'] }),
  });
}
