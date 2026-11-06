package ar.edu.grupo4.accesos.barrio.dto;

import ar.edu.grupo4.accesos.common.security.PasswordSegura;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** US-13: datos para crear un Operador (RF-03). La contraseña inicial cumple la política de US-44. */
public record OperadorRequest(
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

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
    String telefono,

    @PasswordSegura
    String password
) {}
