package ar.edu.grupo4.accesos.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** US-06: el código del enlace del correo. Se valida al abrir la pantalla, sin consumirlo. */
public record ValidarRecuperoRequest(
    @NotBlank(message = "El enlace de recuperación no es válido o ha vencido")
    @Size(max = 100, message = "El enlace de recuperación no es válido o ha vencido")
    String token
) {
}
