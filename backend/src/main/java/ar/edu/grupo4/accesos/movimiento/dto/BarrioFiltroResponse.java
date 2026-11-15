package ar.edu.grupo4.accesos.movimiento.dto;

/**
 * Barrio para el dropdown de filtro en el historial propio de la Empresa (US-31).
 * Solo barrios donde la empresa está o estuvo habilitada (o donde tiene movimientos).
 */
public record BarrioFiltroResponse(
        Long id,
        String nombre
) {
}
