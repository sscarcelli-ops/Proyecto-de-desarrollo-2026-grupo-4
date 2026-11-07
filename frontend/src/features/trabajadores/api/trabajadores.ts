import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';
import type { ApiSchema } from '../../../shared/api/types';
import type { PageResponse } from '../../../shared/types/page';

// ========================================================================================
// Tipos — generados desde los DTOs del backend (npm run api:types)
// ========================================================================================

export type TrabajadorRequest = ApiSchema<'TrabajadorRequest'>;
export type TrabajadorEditarRequest = ApiSchema<'TrabajadorEditarRequest'>;
export type DocumentoResponse = ApiSchema<'DocumentoResponse'>;
export type TrabajadorResponse = ApiSchema<'TrabajadorResponse'>;
export type TipoDocumento = DocumentoResponse['tipo'];

export interface DocumentoUploadPayload {
  tipo: TipoDocumento;
  archivo: File;
  fechaVencimiento: string;
}

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

/** POST /api/trabajadores/:id/documentos — cargar o renovar documento (US-28) */
export async function subirDocumento(trabajadorId: number, payload: DocumentoUploadPayload): Promise<TrabajadorResponse> {
  const formData = new FormData();
  formData.append('tipo', payload.tipo);
  formData.append('archivo', payload.archivo);
  formData.append('fechaVencimiento', payload.fechaVencimiento);

  // Sobrescribe el default 'application/json' de httpClient para enviar multipart (igual que en perfiles.ts)
  const { data } = await httpClient.post<TrabajadorResponse>(
    `/trabajadores/${trabajadorId}/documentos`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  );
  return data;
}

/** DELETE /api/trabajadores/:id/documentos/:tipo — eliminar documento (US-28) */
export async function eliminarDocumento(trabajadorId: number, tipo: TipoDocumento): Promise<TrabajadorResponse> {
  const { data } = await httpClient.delete<TrabajadorResponse>(
    `/trabajadores/${trabajadorId}/documentos/${tipo}`
  );
  return data;
}

/** POST /api/trabajadores/:id/foto — cargar o reemplazar foto (US-25, US-26) */
export async function subirFoto(trabajadorId: number, foto: File): Promise<TrabajadorResponse> {
  const formData = new FormData();
  formData.append('foto', foto);

  // Sobrescribe el default 'application/json' de httpClient para enviar multipart (igual que en perfiles.ts)
  const { data } = await httpClient.post<TrabajadorResponse>(
    `/trabajadores/${trabajadorId}/foto`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  );
  return data;
}

/** DELETE /api/trabajadores/:id/foto — eliminar foto (US-26) */
export async function eliminarFoto(trabajadorId: number): Promise<TrabajadorResponse> {
  const { data } = await httpClient.delete<TrabajadorResponse>(
    `/trabajadores/${trabajadorId}/foto`
  );
  return data;
}

/** GET /api/trabajadores/:id/foto — obtener foto como blob URL para visualización autenticada */
export async function obtenerFotoBlob(trabajadorId: number): Promise<string> {
  const response = await httpClient.get<Blob>(`/trabajadores/${trabajadorId}/foto`, {
    responseType: 'blob',
  });
  return window.URL.createObjectURL(new Blob([response.data]));
}

/** GET /api/trabajadores/:id/documentos/:tipo/archivo — descargar PDF (US-26, US-28) */
export async function descargarDocumento(trabajadorId: number, tipo: TipoDocumento, nombreArchivo?: string): Promise<void> {
  const response = await httpClient.get<Blob>(
    `/trabajadores/${trabajadorId}/documentos/${tipo}/archivo`,
    { responseType: 'blob' }
  );
  const blob = new Blob([response.data], { type: 'application/pdf' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = nombreArchivo || `${tipo.toLowerCase()}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
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

export function useCargarDocumento(trabajadorId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: DocumentoUploadPayload) => subirDocumento(trabajadorId, payload),
    onSuccess: (data) => {
      queryClient.setQueryData([...TRABAJADORES_KEY, trabajadorId], data);
      queryClient.invalidateQueries({ queryKey: TRABAJADORES_KEY });
    },
  });
}

export function useEliminarDocumento(trabajadorId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (tipo: TipoDocumento) => eliminarDocumento(trabajadorId, tipo),
    onSuccess: (data) => {
      queryClient.setQueryData([...TRABAJADORES_KEY, trabajadorId], data);
      queryClient.invalidateQueries({ queryKey: TRABAJADORES_KEY });
    },
  });
}

export function useCargarFoto(trabajadorId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (foto: File) => subirFoto(trabajadorId, foto),
    onSuccess: (data) => {
      queryClient.setQueryData([...TRABAJADORES_KEY, trabajadorId], data);
      queryClient.invalidateQueries({ queryKey: TRABAJADORES_KEY });
    },
  });
}

export function useEliminarFoto(trabajadorId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => eliminarFoto(trabajadorId),
    onSuccess: (data) => {
      queryClient.setQueryData([...TRABAJADORES_KEY, trabajadorId], data);
      queryClient.invalidateQueries({ queryKey: TRABAJADORES_KEY });
    },
  });
}
