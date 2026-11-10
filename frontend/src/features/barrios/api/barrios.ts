import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { FormInstance } from 'antd';
import { httpClient } from '../../../shared/lib/httpClient';
import { getApiProblem, getErrorMessage, toFormFieldErrors } from '../../../shared/lib/apiError';
import { operadoresKeys } from '../../operadores/api/operadores';
import type { ApiSchema } from '../../../shared/api/types';
import { textoUbicacion } from '../../ubicaciones/api/ubicaciones';

// Tipos generados desde los DTO del backend (npm run api:types)
export type Barrio = ApiSchema<'BarrioResponse'>;
export type BarrioRequest = ApiSchema<'BarrioRequest'>;
export type AsignarOperadorRequest = ApiSchema<'AsignarOperadorRequest'>;

/**
 * El barrio con su ubicación, para los mensajes: puede haber dos barrios con el mismo nombre en distintas
 * localidades. Ej. "Barrio Los Álamos (Buenos Aires, Pilar)".
 */
export function nombreConUbicacion(barrio: Barrio): string {
  const ubicacion = textoUbicacion(barrio.provincia, barrio.localidad);
  return ubicacion ? `${barrio.nombre} (${ubicacion})` : barrio.nombre;
}

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

// Barrio de la sesión (Operador o Guardia), para el encabezado "Nombre del Barrio" de sus pantallas
export type MiBarrio = ApiSchema<'MiBarrioResponse'>;

async function getMiBarrio(): Promise<MiBarrio> {
  const { data } = await httpClient.get<MiBarrio>('/barrio');
  return data;
}

export function useMiBarrio() {
  return useQuery({
    queryKey: ['barrio', 'actual'],
    queryFn: getMiBarrio,
    staleTime: Infinity, // el barrio de la sesión no cambia mientras dura
  });
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

// US-11: modifica nombre, localidad (T-11) y dirección (no el estado ni el Operador)
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

// US-45: asigna al barrio un Operador disponible o uno nuevo (se crea en la misma operación) y lo activa
async function asignarOperador(id: number, request: AsignarOperadorRequest): Promise<Barrio> {
  const { data } = await httpClient.put<Barrio>(`/barrios/${id}/operador`, request);
  return data;
}

export function useAsignarOperador(barrioId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: AsignarOperadorRequest) => asignarOperador(barrioId, request),
    // También si falla: si otro Administrador tomó al Operador, la lista de disponibles se actualiza
    onSettled: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: barriosKeys.all }),
      queryClient.invalidateQueries({ queryKey: operadoresKeys.all }),
    ]),
  });
}

/**
 * Marca en el formulario los errores que devuelve el backend: el barrio repetido (mismo nombre en la misma
 * localidad/provincia) va debajo del campo nombre y el resto de las validaciones en su propio campo.
 * Devuelve true si pudo marcar algo en el formulario; si no, el que llama muestra el mensaje general.
 */
export function marcarErroresDeBarrio(form: FormInstance, error: unknown): boolean {
  if (getApiProblem(error)?.code === 'BARRIO_DUPLICADO') {
    form.setFields([{ name: 'nombre', errors: [getErrorMessage(error)] }]);
    return true;
  }
  const fieldErrors = toFormFieldErrors(error);
  if (fieldErrors.length > 0) {
    form.setFields(fieldErrors);
    return true;
  }
  return false;
}
