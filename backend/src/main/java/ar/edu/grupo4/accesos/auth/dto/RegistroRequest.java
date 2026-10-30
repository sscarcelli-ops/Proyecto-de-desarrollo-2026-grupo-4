package ar.edu.grupo4.accesos.auth.dto;

import ar.edu.grupo4.accesos.common.security.PasswordSegura;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * US-02: datos recibidos para crear una cuenta (paso 1 del registro).
 * La contraseña se valida con @PasswordSegura (US-44).
 */
public record RegistroRequest(
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de e-mail no válido")
    String email,

    @PasswordSegura
    String password
) {}
