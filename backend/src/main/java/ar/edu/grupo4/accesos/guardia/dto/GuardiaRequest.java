package ar.edu.grupo4.accesos.guardia.dto;

import ar.edu.grupo4.accesos.common.security.PasswordSegura;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** US-32: datos para crear un Guardia (RF-14). La contraseña inicial cumple la política de US-44. */
public record GuardiaRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    String nombre,

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
    String apellido,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de e-mail no válido")
    @Size(max = 255, message = "El email no puede superar los 255 caracteres")
    String email,

    @PasswordSegura
    String password
) {}
