package ar.edu.grupo4.accesos.trabajador.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos editables de un trabajador existente (US-26).
 *
 * Solo nombre y apellido: el DNI identifica al trabajador dentro de la Empresa
 * y no se puede cambiar. Si la Empresa se equivocó de DNI, da de baja al
 * trabajador y registra uno nuevo.
 *
 * La edición de foto y documentación se maneja por separado (US-28, S2) porque
 * involucra archivos (multipart), no JSON.
 */
public record TrabajadorEditarRequest(
    @NotBlank @Size(max = 100) String nombre,
    @NotBlank @Size(max = 100) String apellido
) {}
