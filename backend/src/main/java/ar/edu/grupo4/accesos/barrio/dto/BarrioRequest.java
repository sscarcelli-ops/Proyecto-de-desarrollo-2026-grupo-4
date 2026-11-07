package ar.edu.grupo4.accesos.barrio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para dar de alta (US-10) o modificar (US-11) un barrio. La ubicación es una localidad del catálogo
 * (T-11), que ya trae su provincia: la provincia se elige en la pantalla solo para filtrar las localidades.
 * Los largos máximos son los de la base (V202611070100): el formulario no deja escribir de más.
 */
public record BarrioRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
    String nombre,

    @NotBlank(message = "La localidad es obligatoria")
    @Size(max = 10, message = "La localidad no existe")
    String localidadId,

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 120, message = "La dirección no puede superar los 120 caracteres")
    String direccion
) {}
