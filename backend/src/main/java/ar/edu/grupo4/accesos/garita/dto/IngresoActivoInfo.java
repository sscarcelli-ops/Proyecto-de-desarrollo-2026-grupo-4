package ar.edu.grupo4.accesos.garita.dto;

/**
 * Información del ingreso activo detectado para este DNI en el barrio.
 * Presente en {@link BusquedaDniResponse} cuando el trabajador (por cualquier vínculo)
 * ya tiene un INGRESO sin EGRESO posterior en ese barrio.
 */
public record IngresoActivoInfo(
        Long movimientoId,
        Long trabajadorId,
        /** Razón social o nombre del monotributista */
        String nombreVinculo,
        /** EMPRESA o MONOTRIBUTISTA */
        String tipo
) {}
