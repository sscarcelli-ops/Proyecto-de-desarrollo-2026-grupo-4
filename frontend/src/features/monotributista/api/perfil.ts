import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';
import type { ApiSchema } from '../../../shared/api/types';

// ========================================================================================
// Tipos — generados desde los DTOs del backend (npm run api:types)
// ========================================================================================

export type MonotributistaPerfilResponse = ApiSchema<'MonotributistaPerfilResponse'>;
export type MonotributistaPerfilModificarRequest = ApiSchema<'MonotributistaPerfilModificarRequest'>;
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

export const MONOTRIBUTISTA_PERFIL_KEY = ['monotributista', 'perfil'] as const;

// ========================================================================================
// Funciones de fetch (llamadas HTTP puras)
// ========================================================================================

/** GET /api/monotributista/perfil — obtener datos personales del Monotributista (US-24) */
async function obtenerMiPerfil(): Promise<MonotributistaPerfilResponse> {
  const { data } = await httpClient.get<MonotributistaPerfilResponse>('/monotributista/perfil');
  return data;
}

/** PUT /api/monotributista/perfil — modificar nombre y apellido (US-24, CA-6) */
async function modificarMiPerfil(
  body: MonotributistaPerfilModificarRequest
): Promise<MonotributistaPerfilResponse> {
  const { data } = await httpClient.put<MonotributistaPerfilResponse>('/monotributista/perfil', body);
  return data;
}

/** POST /api/monotributista/perfil/foto — cargar o actualizar foto de perfil (US-24) */
export async function subirFoto(foto: File): Promise<TrabajadorResponse> {
  const formData = new FormData();
  formData.append('foto', foto);

  const { data } = await httpClient.post<TrabajadorResponse>(
    '/monotributista/perfil/foto',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  );
  return data;
}

/** DELETE /api/monotributista/perfil/foto — eliminar foto de perfil (US-24) */
export async function eliminarFoto(): Promise<TrabajadorResponse> {
  const { data } = await httpClient.delete<TrabajadorResponse>('/monotributista/perfil/foto');
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
export async function subirDocumento(payload: DocumentoUploadPayload): Promise<TrabajadorResponse> {
  const formData = new FormData();
  formData.append('tipo', payload.tipo);
  formData.append('archivo', payload.archivo);
  formData.append('fechaVencimiento', payload.fechaVencimiento);

  const { data } = await httpClient.post<TrabajadorResponse>(
    '/monotributista/perfil/documentos',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  );
  return data;
}

/** DELETE /api/monotributista/perfil/documentos/:tipo — eliminar documento (US-24) */
export async function eliminarDocumento(tipo: TipoDocumento): Promise<TrabajadorResponse> {
  const { data } = await httpClient.delete<TrabajadorResponse>(
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
