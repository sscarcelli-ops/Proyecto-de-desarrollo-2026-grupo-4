package ar.edu.grupo4.accesos.trabajador;

/**
 * Estados documentales calculados para un trabajador (US-26, US-28, US-29, RN-03, RN-04).
 *
 * Valores posibles:
 * - VIGENTE: Al menos un documento cargado y ninguno vencido.
 * - VENCIDO: Al menos un documento vencido (vencimiento < hoy).
 * - PENDIENTE_CARGA: No tiene ningún documento cargado.
 */
public enum EstadoDocumental {
    VIGENTE,
    VENCIDO,
    PENDIENTE_CARGA
}
