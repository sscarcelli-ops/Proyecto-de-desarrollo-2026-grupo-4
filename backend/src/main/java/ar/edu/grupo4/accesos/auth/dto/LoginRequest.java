package ar.edu.grupo4.accesos.auth.dto;

import jakarta.validation.constraints.NotBlank;

// Sin @Email ni política de contraseña: en el login solo se compara contra lo guardado (la política
// se aplica al DEFINIR una contraseña, US-44).
public record LoginRequest(
    @NotBlank String email,
    @NotBlank String password
) {
}
