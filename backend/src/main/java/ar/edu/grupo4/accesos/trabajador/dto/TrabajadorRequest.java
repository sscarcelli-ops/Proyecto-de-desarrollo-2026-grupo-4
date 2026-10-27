package ar.edu.grupo4.accesos.trabajador.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un nuevo trabajador (US-25). Los tres campos son obligatorios;
 * foto y documentación se agregan después del alta (US-28, S2).
 *
 * El DNI se envía "sin puntos" (placeholder del wireframe). No se valida formato
 * porque puede incluir letras en documentos extranjeros; solo que no esté vacío
 * y no supere el largo de la columna.
 */
public record TrabajadorRequest(
    @NotBlank @Size(max = 100) String nombre,
    @NotBlank @Size(max = 100) String apellido,
    @NotBlank @Size(max = 10)  String dni
) {}
