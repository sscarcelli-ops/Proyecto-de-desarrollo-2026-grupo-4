import { useEffect } from 'react';
import { useSearchParams } from 'react-router';

type Valor = string | number | null | undefined;

/**
 * Página, búsqueda y filtros de un listado guardados en la dirección (ej. /empresas?pagina=3&estado=PENDIENTE)
 * en lugar de en un useState. Así, al recargar o al volver con "Atrás" el listado sigue donde estaba, y el link
 * se puede copiar tal cual.
 *
 *   const url = useParametrosUrl();
 *   const pagina = url.numero('pagina', 1);
 *   const busqueda = url.texto('q');
 *   url.cambiar({ q: 'acme', pagina: null }); // null o '' borra el parámetro (la página vuelve a la 1)
 *
 * Varios cambios van en UNA llamada a cambiar(): dos llamadas seguidas se pisan, porque cada una parte de la
 * dirección que había antes del clic.
 */
export function useParametrosUrl() {
  const [params, setParams] = useSearchParams();

  /** Texto del parámetro, o '' si no está. */
  const texto = (nombre: string): string => params.get(nombre) ?? '';

  /** Número entero positivo del parámetro; si no está o es inválido (ej. ?pagina=abc), el valor por defecto. */
  const numero = (nombre: string, porDefecto: number): number => {
    const n = Number(params.get(nombre));
    return Number.isInteger(n) && n > 0 ? n : porDefecto;
  };

  const cambiar = (cambios: Record<string, Valor>) => {
    const nuevos = new URLSearchParams(params);
    for (const [nombre, valor] of Object.entries(cambios)) {
      if (valor === null || valor === undefined || valor === '') nuevos.delete(nombre);
      else nuevos.set(nombre, String(valor));
    }
    // replace: no suma una entrada al historial por cada letra o página; "Atrás" vuelve a la pantalla anterior
    setParams(nuevos, { replace: true });
  };

  return { texto, numero, cambiar };
}

/**
 * Si la página pedida no existe, va a la última con datos en lugar de mostrar el listado vacío. Pasa si alguien
 * escribe ?pagina=99 a mano, o si recarga en la página 3 y mientras tanto se fueron registros.
 *
 *   usePaginaValida(url, pagina, data?.totalPages);
 */
export function usePaginaValida(
  url: ReturnType<typeof useParametrosUrl>,
  pagina: number,
  totalPaginas: number | undefined,
) {
  const ultima = Math.max(1, totalPaginas ?? 1);
  const fueraDeRango = totalPaginas !== undefined && pagina > ultima;
  // En un efecto: cambiar la dirección mientras se dibuja la pantalla no está permitido
  useEffect(() => {
    if (fueraDeRango) url.cambiar({ pagina: ultima });
  });
}
