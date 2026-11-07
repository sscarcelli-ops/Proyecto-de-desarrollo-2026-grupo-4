package ar.edu.grupo4.accesos.barrio.dto;

import ar.edu.grupo4.accesos.barrio.Barrio;
import ar.edu.grupo4.accesos.ubicacion.Localidad;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Barrio para el listado y la edición. La ubicación va separada (T-11) y la pantalla la muestra como
 * "Provincia, Localidad". Es null solo en barrios anteriores a T-11 que no coincidieron con el catálogo.
 */
public record BarrioResponse(
    Long id,
    String nombre,
    @Schema(types = {"string", "null"}) String provinciaId,
    @Schema(types = {"string", "null"}) String provincia,
    @Schema(types = {"string", "null"}) String localidadId,
    @Schema(types = {"string", "null"}) String localidad,
    String direccion,
    String estado,
    @Schema(types = {"integer", "null"}) Long operadorId
) {
    public static BarrioResponse from(Barrio b) {
        Localidad l = b.getLocalidad();
        return new BarrioResponse(b.getId(), b.getNombre(),
            l == null ? null : l.getProvincia().getId(),
            l == null ? null : l.getProvincia().getNombre(),
            l == null ? null : l.getId(),
            l == null ? null : l.getNombre(),
            b.getDireccion(), b.getEstado().name(), b.getOperadorId());
    }
}
