package ar.edu.grupo4.accesos.movimiento.dto;

import java.time.OffsetDateTime;

/**
 * Movimiento en el historial de accesos del barrio (US-36).
 */
public record MovimientoHistorialResponse(
        Long id,
        OffsetDateTime fechaHora,
        String tipo,
        String trabajadorNombre,
        String trabajadorApellido,
        String trabajadorDni,
        String entidadTipo,
        String entidadNombre,
        String motivo
) {
}
