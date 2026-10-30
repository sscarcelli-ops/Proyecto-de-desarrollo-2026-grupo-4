package ar.edu.grupo4.accesos.barrio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos para dar de alta (US-10) o modificar (US-11) un barrio. */
public record BarrioRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    String nombre,

    @NotBlank(message = "La localidad/provincia es obligatoria")
    @Size(max = 150, message = "La localidad/provincia no puede superar los 150 caracteres")
    String localidadProvincia,

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    String direccion
) {}
