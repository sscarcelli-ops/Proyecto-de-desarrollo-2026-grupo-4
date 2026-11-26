package ar.edu.grupo4.accesos.barrio.dto;

import ar.edu.grupo4.accesos.barrio.EstadoOperador;
import ar.edu.grupo4.accesos.barrio.OperadorRepository.OperadorListado;
import io.swagger.v3.oas.annotations.media.Schema;

/** US-15: una fila del listado de Operadores del Administrador. Nunca incluye la contraseña ni su hash. */
public record OperadorListadoResponse(
    Long id,
    String nombre,
    String apellido,
    String email,
    String telefono,
    @Schema(types = {"integer", "null"}) Long barrioId,
    @Schema(types = {"string", "null"}) String barrioNombre,
    String estado
) {
    public static OperadorListadoResponse from(OperadorListado o) {
        EstadoOperador estado = o.getBarrioId() == null ? EstadoOperador.DISPONIBLE : EstadoOperador.OCUPADO;
        return new OperadorListadoResponse(o.getId(), o.getNombre(), o.getApellido(), o.getEmail(), o.getTelefono(),
            o.getBarrioId(), o.getBarrioNombre(), estado.name());
    }
}
