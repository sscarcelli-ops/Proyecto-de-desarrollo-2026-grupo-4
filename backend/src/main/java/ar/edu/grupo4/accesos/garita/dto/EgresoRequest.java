package ar.edu.grupo4.accesos.garita.dto;

import jakarta.validation.constraints.NotNull;

/** Body del POST /api/garita/egreso (US-40). El barrioId y guardiaId vienen del JWT. */
public record EgresoRequest(
        @NotNull(message = "El trabajador es obligatorio") Long trabajadorId
) {}
