package ar.edu.grupo4.accesos.guardia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * US-32: datos para modificar un Guardia. Igual que el alta, pero la contraseña es opcional: en blanco (o sin
 * enviar) no se cambia; si viene, el Service comprueba la política de US-44.
 */
public record GuardiaModificarRequest(
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

    String password
) {}
