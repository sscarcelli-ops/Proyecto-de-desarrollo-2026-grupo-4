package ar.edu.grupo4.accesos.guardia.dto;

import ar.edu.grupo4.accesos.guardia.Guardia;

/** Guardia del barrio. Nunca incluye la contraseña ni su hash. */
public record GuardiaResponse(
    Long id,
    String nombre,
    String apellido,
    String email,
    boolean habilitado
) {
    public static GuardiaResponse from(Guardia g, String email) {
        return new GuardiaResponse(g.getUsuarioId(), g.getNombre(), g.getApellido(), email, g.isHabilitado());
    }
}
