package ar.edu.grupo4.accesos.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Sin @Email ni política de contraseña: en el login solo se compara contra lo guardado (la política
// se aplica al DEFINIR una contraseña, US-44).
public record LoginRequest(
    // Los mismos largos máximos que al crear la cuenta: un texto más largo no puede coincidir con ninguna
    @NotBlank @Size(max = 255) String email,
    @NotBlank @Size(max = 72) String password
) {
}
