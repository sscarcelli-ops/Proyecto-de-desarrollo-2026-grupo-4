package ar.edu.grupo4.accesos.auth.dto;

import ar.edu.grupo4.accesos.common.security.PasswordSegura;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * US-01, pantalla "Registrate": crea el Administrador y consume el código. La repetición de la contraseña
 * se valida en el frontend; acá llega una sola.
 */
public record ActivacionRequest(
    @NotBlank(message = "El código de activación es obligatorio")
    @Size(max = 100, message = "El código de activación no puede superar los 100 caracteres")
    String token,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de e-mail no válido")
    @Size(max = 255, message = "El email no puede superar los 255 caracteres")
    String email,

    @PasswordSegura
    String password
) {
}
