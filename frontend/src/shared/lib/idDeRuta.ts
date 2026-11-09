/**
 * Id numérico de un parámetro de la ruta (/barrios/:id/editar). Devuelve null si no es un entero positivo
 * ("abc", "-1", "1e5", "1; drop"...): la pantalla muestra "página no encontrada" sin llamar al backend.
 */
export function idDeRuta(valor: string | undefined): number | null {
  if (valor === undefined || !/^[1-9]\d{0,17}$/.test(valor)) return null;
  return Number(valor);
}
