import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';
import type { ApiSchema } from '../../../shared/api/types';

// ========================================================================================
// Tipos — DTOs del perfil propio de Monotributista (US-24)
// ========================================================================================

export type MonotributistaPerfilResponse = ApiSchema<'MonotributistaPerfilResponse'>;
export type MonotributistaPerfilModificarRequest = ApiSchema<'MonotributistaPerfilModificarRequest'>;
export type DocumentoResponse = ApiSchema<'DocumentoResponse'>;
export type TipoDocumento = DocumentoResponse['tipo'];

export interface DocumentoUploadPayload {
  tipo: TipoDocumento;
  archivo: File;
  fechaVencimiento: string;
}

// ========================================================================================
// Claves de caché de TanStack Query
// ========================================================================================

export const MONOTRIBUTISTA_PERFIL_KEY = ['monotributista', 'perfil'] as const;

// ========================================================================================
// Funciones HTTP puras
// ========================================================================================

/** GET /api/monotributista/perfil — obtener el perfil propio del Monotributista autenticado (US-24) */
export async function obtenerMiPerfil(): Promise<MonotributistaPerfilResponse> {
  const { data } = await httpClient.get<MonotributistaPerfilResponse>('/monotributista/perfil');
  return data;
}

/** PUT /api/monotributista/perfil — modificar nombre y apellido (US-24, CA-6) */
export async function modificarMiPerfil(
  body: MonotributistaPerfilModificarRequest
): Promise<MonotributistaPerfilResponse> {
  const { data } = await httpClient.put<MonotributistaPerfilResponse>('/monotributista/perfil', body);
  return data;
}

/** POST /api/monotributista/perfil/foto — cargar o actualizar foto de perfil (US-24) */
export async function subirFoto(foto: File): Promise<MonotributistaPerfilResponse> {
  const formData = new FormData();
  formData.append('foto', foto);

  const { data } = await httpClient.post<MonotributistaPerfilResponse>(
    '/monotributista/perfil/foto',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  );
  return data;
}

/** DELETE /api/monotributista/perfil/foto — eliminar foto de perfil (US-24) */
export async function eliminarFoto(): Promise<MonotributistaPerfilResponse> {
  const { data } = await httpClient.delete<MonotributistaPerfilResponse>('/monotributista/perfil/foto');
  return data;
}

/** GET /api/monotributista/perfil/foto — obtener foto como blob URL para visualización autenticada */
export async function obtenerFotoBlob(): Promise<string> {
  const response = await httpClient.get<Blob>('/monotributista/perfil/foto', {
    responseType: 'blob',
  });
  return window.URL.createObjectURL(new Blob([response.data]));
}

/** POST /api/monotributista/perfil/documentos — cargar o renovar documento (ART / Seguro de Vida) (US-24) */
export async function subirDocumento(payload: DocumentoUploadPayload): Promise<MonotributistaPerfilResponse> {
  const formData = new FormData();
  formData.append('tipo', payload.tipo);
  formData.append('archivo', payload.archivo);
  formData.append('fechaVencimiento', payload.fechaVencimiento);

  const { data } = await httpClient.post<MonotributistaPerfilResponse>(
    '/monotributista/perfil/documentos',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  );
  return data;
}

/** DELETE /api/monotributista/perfil/documentos/:tipo — eliminar documento (US-24) */
export async function eliminarDocumento(tipo: TipoDocumento): Promise<MonotributistaPerfilResponse> {
  const { data } = await httpClient.delete<MonotributistaPerfilResponse>(
    `/monotributista/perfil/documentos/${tipo}`
  );
  return data;
}

/** GET /api/monotributista/perfil/documentos/:tipo — descargar PDF (US-24) */
export async function descargarDocumento(tipo: TipoDocumento, nombreArchivo?: string): Promise<void> {
  const response = await httpClient.get<Blob>(
    `/monotributista/perfil/documentos/${tipo}`,
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
// Hooks de TanStack Query
// ========================================================================================

/**
 * Hook para obtener los datos del perfil propio del Monotributista.
 */
export function useMiPerfilMonotributista() {
  return useQuery({
    queryKey: MONOTRIBUTISTA_PERFIL_KEY,
    queryFn: obtenerMiPerfil,
  });
}

/**
 * Hook para modificar datos personales (nombre, apellido) (US-24, CA-6).
 */
export function useModificarPerfilMonotributista() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: MonotributistaPerfilModificarRequest) => modificarMiPerfil(body),
    onSuccess: (data) => {
      queryClient.setQueryData(MONOTRIBUTISTA_PERFIL_KEY, data);
      queryClient.invalidateQueries({ queryKey: MONOTRIBUTISTA_PERFIL_KEY });
    },
  });
}

/**
 * Hook para subir o actualizar la foto de perfil.
 */
export function useCargarFotoMonotributista() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (foto: File) => subirFoto(foto),
    onSuccess: (data) => {
      queryClient.setQueryData(MONOTRIBUTISTA_PERFIL_KEY, data);
      queryClient.invalidateQueries({ queryKey: MONOTRIBUTISTA_PERFIL_KEY });
    },
  });
}

/**
 * Hook para eliminar la foto de perfil.
 */
export function useEliminarFotoMonotributista() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => eliminarFoto(),
    onSuccess: (data) => {
      queryClient.setQueryData(MONOTRIBUTISTA_PERFIL_KEY, data);
      queryClient.invalidateQueries({ queryKey: MONOTRIBUTISTA_PERFIL_KEY });
    },
  });
}

/**
 * Hook para cargar o renovar un documento (ART o Seguro de Vida).
 */
export function useCargarDocumentoMonotributista() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: DocumentoUploadPayload) => subirDocumento(payload),
    onSuccess: (data) => {
      queryClient.setQueryData(MONOTRIBUTISTA_PERFIL_KEY, data);
      queryClient.invalidateQueries({ queryKey: MONOTRIBUTISTA_PERFIL_KEY });
    },
  });
}

/**
 * Hook para eliminar un documento.
 */
export function useEliminarDocumentoMonotributista() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (tipo: TipoDocumento) => eliminarDocumento(tipo),
    onSuccess: (data) => {
      queryClient.setQueryData(MONOTRIBUTISTA_PERFIL_KEY, data);
      queryClient.invalidateQueries({ queryKey: MONOTRIBUTISTA_PERFIL_KEY });
    },
  });
}
