package ar.edu.grupo4.accesos.barrio.dto;

import ar.edu.grupo4.accesos.barrio.Barrio;
import ar.edu.grupo4.accesos.barrio.Operador;
import io.swagger.v3.oas.annotations.media.Schema;

public record BarrioResponse(
    Long id,
    String nombre,
    String localidadProvincia,
    String direccion,
    String estado,
    @Schema(types = {"integer", "null"}) Long operadorId,
    @Schema(types = {"string", "null"}) String operadorNombre,
    @Schema(types = {"string", "null"}) String operadorApellido
) {
    /** El Operador es null si el barrio no tiene (nombre y apellido salen null). */
    public static BarrioResponse from(Barrio b, Operador operador) {
        return new BarrioResponse(b.getId(), b.getNombre(), b.getLocalidadProvincia(), b.getDireccion(),
            b.getEstado().name(), b.getOperadorId(),
            operador == null ? null : operador.getNombre(),
            operador == null ? null : operador.getApellido());
    }
}
