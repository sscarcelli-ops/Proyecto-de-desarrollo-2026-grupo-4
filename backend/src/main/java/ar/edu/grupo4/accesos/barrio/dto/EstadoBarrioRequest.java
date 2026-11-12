package ar.edu.grupo4.accesos.barrio.dto;

import jakarta.validation.constraints.NotNull;

/** US-12: activar (true) o desactivar (false) un barrio. */
public record EstadoBarrioRequest(
    @NotNull(message = "Indicá si el barrio queda activo o inactivo")
    Boolean activo
) {}
