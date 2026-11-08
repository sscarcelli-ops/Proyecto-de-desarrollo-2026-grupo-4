package ar.edu.grupo4.accesos.barrio.dto;

import ar.edu.grupo4.accesos.barrio.Operador;

/** Operador creado. Nunca incluye la contraseña ni su hash. */
public record OperadorResponse(
    Long id,
    String nombre,
    String apellido,
    String email,
    String telefono
) {
    public static OperadorResponse from(Operador o, String email) {
        return new OperadorResponse(o.getUsuarioId(), o.getNombre(), o.getApellido(), email, o.getTelefono());
    }
}
