package ar.edu.grupo4.accesos.auth.dto;

import ar.edu.grupo4.accesos.common.security.PasswordSegura;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * US-06: nueva contraseña con el código del enlace. La política es la de US-44 (RNF-09).
 * La repetición de la contraseña se valida en el frontend; acá llega una sola.
 */
public record RestablecerPasswordRequest(
    @NotBlank(message = "El enlace de recuperación no es válido o ha vencido")
    @Size(max = 100, message = "El enlace de recuperación no es válido o ha vencido")
    String token,

    @PasswordSegura
    String password
) {
}
