package ar.edu.grupo4.accesos.movimiento.dto;

/**
 * Entidad (Empresa o Monotributista) para el filtro desplegable del historial (US-36).
 */
public record EntidadFiltroResponse(
        Long id,
        String nombre,
        String tipo
) {
}
