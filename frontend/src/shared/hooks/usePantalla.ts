import { Grid } from 'antd';

/** Si la pantalla cumple la media query ahora mismo (sin esperar a que React vuelva a dibujar). */
function coincide(media: string): boolean {
  return typeof window !== 'undefined' && window.matchMedia(media).matches;
}

/**
 * T-07: tamaño de pantalla, con los mismos puntos de corte de Ant Design:
 * - celular: menos de 768 px (md);
 * - escritorio: 992 px o más (lg); en el medio, tablet.
 *
 * Grid.useBreakpoint() de Ant Design no sabe el tamaño en el primer dibujo (devuelve {}), así que una pantalla
 * que lo usa directo arranca en un modo y enseguida cambia al correcto: el menú se ve ancho y se achica, o una
 * tabla aparece y pasa a tarjetas. Este hook usa matchMedia para ese primer dibujo y después sigue a Ant Design,
 * que avisa cuando cambia el tamaño (girar la tablet, achicar la ventana).
 */
export function usePantalla() {
  const puntos = Grid.useBreakpoint();
  const md = puntos.md ?? coincide('(min-width: 768px)');
  const lg = puntos.lg ?? coincide('(min-width: 992px)');
  return { esCelular: !md, esEscritorio: lg };
}
