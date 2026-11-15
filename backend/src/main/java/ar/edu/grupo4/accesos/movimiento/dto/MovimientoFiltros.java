package ar.edu.grupo4.accesos.movimiento.dto;

import java.time.LocalDate;

/**
 * Filtros de búsqueda para el historial de accesos del barrio (US-36).
 */
public record MovimientoFiltros(
        LocalDate desde,
        LocalDate hasta,
        String dni,
        Long perfilId
) {
}
