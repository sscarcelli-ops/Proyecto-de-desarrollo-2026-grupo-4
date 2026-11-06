import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { httpClient } from '../../../shared/lib/httpClient';
import { authToken } from '../../../shared/lib/authToken';
import type { ApiSchema } from '../../../shared/api/types';
import type { PageResponse } from '../../../shared/types/page';

export type PerfilResponse = ApiSchema<'PerfilResponse'>;

export const MI_SOLICITUD_KEY = ['perfil', 'mi-solicitud'] as const;
export const PERFILES_KEY = ['perfiles'] as const;

export function useMiSolicitud(options?: { enabled?: boolean }) {
  const token = authToken.get();
  const enabledByToken = token !== null;
  const enabled = enabledByToken && (options?.enabled ?? true);
  return useQuery({
    queryKey: MI_SOLICITUD_KEY,
    queryFn: async () => {
      try {
        const res = await httpClient.get<PerfilResponse>('/perfil/mi-solicitud');
        return res.data;
      } catch (err: unknown) {
        // Si no tiene perfil aún (404), devuelve null en vez de romper la query
        const e = err as { response?: { status?: number } };
        if (e.response?.status === 404) {
          return null;
        }
        throw err;
      }
    },
    enabled,
    retry: false,
  });
}

export function useSolicitarPerfil() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (formData: FormData) => {
      const res = await httpClient.post<PerfilResponse>('/perfil', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      return res.data;
    },
    onSuccess: (data) => {
      queryClient.setQueryData(MI_SOLICITUD_KEY, data);
    },
  });
}

// =========================================================================
// US-18: Listado y Detalle de Empresas y Monotributistas (Admin)
// =========================================================================

export interface FiltrosPerfiles {
  page: number;
  size?: number;
  q?: string;
  estado?: PerfilResponse['estado'];
}

async function listarPerfiles({
  page,
  size = 10,
  q,
  estado,
}: FiltrosPerfiles): Promise<PageResponse<PerfilResponse>> {
  const params: Record<string, unknown> = {
    page: page - 1, // Spring cuenta desde 0
    size,
  };
  if (q && q.trim()) {
    params.q = q.trim();
  }
  if (estado) {
    params.estado = estado;
  }
  const { data } = await httpClient.get<PageResponse<PerfilResponse>>('/perfiles', { params });
  return data;
}

export function usePerfiles(filtros: FiltrosPerfiles) {
  return useQuery({
    queryKey: [...PERFILES_KEY, filtros],
    queryFn: () => listarPerfiles(filtros),
    placeholderData: (prev) => prev,
  });
}

export function usePerfil(id: number) {
  return useQuery({
    queryKey: [...PERFILES_KEY, id],
    queryFn: async () => {
      const { data } = await httpClient.get<PerfilResponse>(`/perfiles/${id}`);
      return data;
    },
    enabled: id > 0,
  });
}

/**
 * Descarga o visualiza la constancia de inscripción en PDF mediante el cliente autenticado.
 * Abre la nueva pestaña de inmediato durante el gesto del usuario para evitar bloqueos del navegador.
 * En caso de error, si la respuesta viene encapsulada como Blob se parsea a JSON para mostrar el detalle del backend.
 */
export async function verConstanciaPdf(id: number): Promise<void> {
  const nuevaVentana = window.open('', '_blank');
  try {
    const response = await httpClient.get<Blob>(`/perfiles/${id}/constancia`, {
      responseType: 'blob',
    });
    const blob = new Blob([response.data], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    if (nuevaVentana) {
      nuevaVentana.location.href = url;
    } else {
      const enlace = document.createElement('a');
      enlace.href = url;
      enlace.target = '_blank';
      enlace.rel = 'noopener noreferrer';
      document.body.appendChild(enlace);
      enlace.click();
      enlace.remove();
    }
  } catch (error) {
    if (nuevaVentana) {
      nuevaVentana.close();
    }
    if (axios.isAxiosError(error) && error.response?.data instanceof Blob) {
      try {
        const texto = await error.response.data.text();
        error.response.data = JSON.parse(texto);
      } catch {
        // mantener el error original si no era JSON
      }
    }
    throw error;
  }
}

// =========================================================================
// US-20: Aprobar y Rechazar Solicitudes
// =========================================================================

export function useAprobarPerfil() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      const { data } = await httpClient.post<PerfilResponse>(`/perfiles/${id}/aprobar`);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PERFILES_KEY });
      queryClient.invalidateQueries({ queryKey: MI_SOLICITUD_KEY });
    },
  });
}

export function useRechazarPerfil() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, motivo }: { id: number; motivo: string }) => {
      const { data } = await httpClient.post<PerfilResponse>(`/perfiles/${id}/rechazar`, {
        motivo,
      });
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PERFILES_KEY });
      queryClient.invalidateQueries({ queryKey: MI_SOLICITUD_KEY });
    },
  });
}
