package ar.edu.grupo4.accesos.barrio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos para dar de alta (US-10) o modificar (US-11) un barrio. */
public record BarrioRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
    String nombre,

    @NotBlank(message = "La localidad/provincia es obligatoria")
    @Size(max = 80, message = "La localidad/provincia no puede superar los 80 caracteres")
    String localidadProvincia,

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 120, message = "La dirección no puede superar los 120 caracteres")
    String direccion
) {}
