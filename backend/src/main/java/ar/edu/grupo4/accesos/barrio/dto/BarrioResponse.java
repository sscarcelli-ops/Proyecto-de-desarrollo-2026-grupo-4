package ar.edu.grupo4.accesos.barrio.dto;

import ar.edu.grupo4.accesos.barrio.Barrio;
import io.swagger.v3.oas.annotations.media.Schema;

public record BarrioResponse(
    Long id,
    String nombre,
    String localidadProvincia,
    String direccion,
    String estado,
    @Schema(types = {"integer", "null"}) Long operadorId
) {
    public static BarrioResponse from(Barrio b) {
        return new BarrioResponse(b.getId(), b.getNombre(), b.getLocalidadProvincia(),
            b.getDireccion(), b.getEstado().name(), b.getOperadorId());
    }
}
