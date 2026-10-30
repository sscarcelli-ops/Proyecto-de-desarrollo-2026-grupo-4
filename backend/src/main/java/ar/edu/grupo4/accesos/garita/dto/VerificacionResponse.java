package ar.edu.grupo4.accesos.garita.dto;

/** Resultado de la función verificar_acceso (US-38). */
public record VerificacionResponse(
        boolean habilitado,
        String motivo
) {}
