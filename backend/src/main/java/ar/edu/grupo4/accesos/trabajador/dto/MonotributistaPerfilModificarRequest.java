package ar.edu.grupo4.accesos.trabajador.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Petición de modificación de datos personales del Monotributista (US-24, CA-6).
 *
 * Solo permite modificar nombre, apellido y teléfono de contacto.
 * El DNI y el CUIL no forman parte de este DTO, asegurando su inmutabilidad.
 */
public record MonotributistaPerfilModificarRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    String nombre,

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
    String apellido,

    @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
    String telefono
) {
}
