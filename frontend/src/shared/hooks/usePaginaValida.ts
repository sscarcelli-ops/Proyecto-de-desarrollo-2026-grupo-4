import { useEffect } from 'react';
import type { ParametrosUrl } from './useParametrosUrl';

/**
 * Hook complementario a useParametrosUrl (GUIA-DESARROLLO.md §5.2).
 * Si la página actual en la URL excede el total de páginas disponibles devueltas
 * por el backend (por ejemplo alguien escribe ?pagina=99), corrige automáticamente
 * la dirección llevándola a la última página válida con datos.
 */
export function usePaginaValida(
  url: ParametrosUrl,
  paginaActual: number,
  totalPaginas: number | undefined
) {
  useEffect(() => {
    if (totalPaginas === undefined || totalPaginas <= 0) return;

    if (paginaActual > totalPaginas) {
      url.cambiar({ pagina: totalPaginas === 1 ? null : totalPaginas });
    }
  }, [url, paginaActual, totalPaginas]);
}
