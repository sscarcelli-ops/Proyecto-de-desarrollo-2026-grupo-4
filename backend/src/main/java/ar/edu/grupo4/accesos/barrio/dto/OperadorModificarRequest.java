package ar.edu.grupo4.accesos.barrio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * US-16: datos que el Administrador puede cambiar de un Operador (RF-32): nombre, apellido y teléfono. No lleva
 * email: es el usuario de acceso y solo se visualiza. Si el pedido trae un email, se ignora.
 */
public record OperadorModificarRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    String nombre,

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
    String apellido,

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
    String telefono
) {}
