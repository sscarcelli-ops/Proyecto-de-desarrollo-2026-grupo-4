package ar.edu.grupo4.accesos.garita.dto;

import jakarta.validation.constraints.NotNull;

/** Body del POST /api/garita/verificar (US-38). El barrioId viene del JWT. */
public record VerificacionRequest(
        @NotNull(message = "El trabajador es obligatorio") Long trabajadorId
) {}
