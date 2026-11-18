import { useCallback } from 'react';
import { useSearchParams } from 'react-router';

/**
 * Hook para sincronizar parámetros de búsqueda, filtros y paginación en la URL.
 * Cumple con GUIA-DESARROLLO.md §5.2.
 *
 * Características:
 * - url.numero('pagina', 1): parsea un entero positivo (> 0). Valores inválidos ('abc', -3, 0) devuelven el default.
 * - url.texto('q', ''): devuelve el texto del parámetro o el default.
 * - url.booleano('bajas', false): parsea booleanos ('true' / '1' -> true; 'false' / '0' -> false).
 * - url.cambiar(nuevos): actualiza parámetros atómicamente en una sola llamada para evitar pisadas.
 *   Si el valor es null, undefined o '' (cadena vacía), se elimina de la URL.
 */
export function useParametrosUrl() {
  const [searchParams, setSearchParams] = useSearchParams();

  const numero = useCallback(
    (clave: string, porDefecto: number): number => {
      const valor = searchParams.get(clave);
      if (!valor) return porDefecto;
      const num = Number(valor);
      return Number.isInteger(num) && num > 0 ? num : porDefecto;
    },
    [searchParams]
  );

  const texto = useCallback(
    (clave: string, porDefecto: string = ''): string => {
      return searchParams.get(clave) ?? porDefecto;
    },
    [searchParams]
  );

  const booleano = useCallback(
    (clave: string, porDefecto: boolean = false): boolean => {
      const valor = searchParams.get(clave);
      if (valor === null || valor === undefined || valor === '') return porDefecto;
      if (valor === 'true' || valor === '1') return true;
      if (valor === 'false' || valor === '0') return false;
      return porDefecto;
    },
    [searchParams]
  );

  const cambiar = useCallback(
    (nuevos: Record<string, string | number | boolean | null | undefined>) => {
      setSearchParams(
        (prev) => {
          const params = new URLSearchParams(prev);
          for (const [clave, valor] of Object.entries(nuevos)) {
            if (valor === null || valor === undefined || valor === '') {
              params.delete(clave);
            } else {
              params.set(clave, String(valor));
            }
          }
          return params;
        },
        { replace: true }
      );
    },
    [setSearchParams]
  );

  return {
    numero,
    texto,
    booleano,
    cambiar,
    searchParams,
  };
}

export type ParametrosUrl = ReturnType<typeof useParametrosUrl>;
