package ar.edu.grupo4.accesos.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** US-06: email de la cuenta que olvidó su contraseña. */
public record SolicitarRecuperoRequest(
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de e-mail no válido")
    @Size(max = 255, message = "El email no puede superar los 255 caracteres")
    String email
) {
}
