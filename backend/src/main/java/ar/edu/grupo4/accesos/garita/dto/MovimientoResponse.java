package ar.edu.grupo4.accesos.garita.dto;

import java.time.OffsetDateTime;

/** Confirmación de un ingreso o egreso registrado. */
public record MovimientoResponse(
        Long id,
        /** INGRESO o EGRESO */
        String tipo,
        OffsetDateTime fechaHora,
        String nombreTrabajador,
        String nombreVinculo
) {}
