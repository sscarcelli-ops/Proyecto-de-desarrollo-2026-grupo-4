package ar.edu.grupo4.accesos.trabajador.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un nuevo trabajador (US-25). Los tres campos son obligatorios;
 * foto y documentación se agregan después del alta (US-28, S2).
 *
 * El DNI se valida con 7 u 8 dígitos numéricos ("sin puntos").
 */
public record TrabajadorRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    String nombre,

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
    String apellido,

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^[0-9]{7,8}$", message = "El DNI debe contener 7 u 8 dígitos numéricos, sin puntos")
    String dni
) {}
