package ar.edu.grupo4.accesos.movimiento.dto;

import java.time.OffsetDateTime;

/**
 * Movimiento en el historial propio de la Empresa o Monotributista (US-31 y US-46).
 */
public record MovimientoPropioResponse(
        Long id,
        OffsetDateTime fechaHora,
        String tipo,
        String trabajadorNombre,
        String trabajadorApellido,
        String trabajadorDni,
        String barrioNombre,
        String motivo
) {
}
