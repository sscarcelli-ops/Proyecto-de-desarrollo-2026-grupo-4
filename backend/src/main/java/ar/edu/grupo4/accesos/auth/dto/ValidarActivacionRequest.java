package ar.edu.grupo4.accesos.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** US-01, pantalla "Activación de cuenta": el código se valida pero no se consume. */
public record ValidarActivacionRequest(
    @NotBlank(message = "El código de activación es obligatorio")
    @Size(max = 100, message = "El código de activación no puede superar los 100 caracteres")
    String token
) {
}
