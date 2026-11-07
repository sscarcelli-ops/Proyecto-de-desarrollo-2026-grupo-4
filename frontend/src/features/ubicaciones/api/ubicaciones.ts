import { useQuery } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';
import type { ApiSchema } from '../../../shared/api/types';

// T-11: catálogo de provincias y localidades (datos oficiales de Georef, cargados en la base)
export type Provincia = ApiSchema<'ProvinciaResponse'>;
export type Localidad = ApiSchema<'LocalidadResponse'>;

export const ubicacionesKeys = {
  provincias: ['ubicaciones', 'provincias'] as const,
  localidades: (provinciaId: string) => ['ubicaciones', 'provincias', provinciaId, 'localidades'] as const,
};

async function getProvincias(): Promise<Provincia[]> {
  const { data } = await httpClient.get<Provincia[]>('/ubicaciones/provincias');
  return data;
}

async function getLocalidades(provinciaId: string): Promise<Localidad[]> {
  const { data } = await httpClient.get<Localidad[]>(`/ubicaciones/provincias/${provinciaId}/localidades`);
  return data;
}

// El catálogo no cambia mientras la app está abierta: se pide una sola vez
export function useProvincias() {
  return useQuery({ queryKey: ubicacionesKeys.provincias, queryFn: getProvincias, staleTime: Infinity });
}

export function useLocalidades(provinciaId: string | undefined) {
  return useQuery({
    queryKey: ubicacionesKeys.localidades(provinciaId ?? ''),
    queryFn: () => getLocalidades(provinciaId as string),
    enabled: !!provinciaId,
    staleTime: Infinity,
  });
}

/** Cómo se muestra una ubicación en toda la app: "Buenos Aires, Baradero". */
export function textoUbicacion(provincia: string | null | undefined, localidad: string | null | undefined) {
  return provincia && localidad ? `${provincia}, ${localidad}` : null;
}
