package ar.edu.grupo4.accesos.auth.dto;

import ar.edu.grupo4.accesos.common.security.PasswordSegura;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * US-07: la contraseña actual solo se compara contra la guardada; la política se aplica a la nueva (US-44).
 * La repetición de la nueva se valida en el frontend; acá llega una sola.
 */
public record CambiarPasswordRequest(
    @NotBlank(message = "La contraseña actual es obligatoria")
    @Size(max = 72, message = "La contraseña no puede tener más de 72 caracteres")
    String passwordActual,

    @PasswordSegura
    String passwordNueva
) {
}
