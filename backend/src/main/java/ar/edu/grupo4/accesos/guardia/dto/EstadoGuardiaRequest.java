package ar.edu.grupo4.accesos.guardia.dto;

import jakarta.validation.constraints.NotNull;

/** US-32: habilitar (true) o deshabilitar (false) un Guardia. */
public record EstadoGuardiaRequest(
    @NotNull(message = "Indicá si el Guardia queda habilitado o deshabilitado")
    Boolean habilitado
) {}
