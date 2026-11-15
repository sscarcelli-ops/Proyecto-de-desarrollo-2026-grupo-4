package ar.edu.grupo4.accesos.movimiento.dto;

import java.time.LocalDate;

/**
 * Filtros de consulta de historial de accesos propio (US-31 y US-46).
 */
public record MovimientoPropioFiltros(
        LocalDate desde,
        LocalDate hasta,
        String dni,
        Long barrioId
) {
}
