package ar.edu.grupo4.accesos.garita.dto;

/**
 * Representa un vínculo laboral encontrado al buscar por DNI.
 * Un trabajador puede tener múltiples vínculos (Empresa + Monotributista propio).
 */
public record VinculoResultado(
        Long trabajadorId,
        Long perfilId,
        /** Razón social o "Juan Pérez (Monotributista)" */
        String nombreVinculo,
        /** EMPRESA o MONOTRIBUTISTA */
        String tipo
) {}
