package ar.edu.grupo4.accesos.garita.dto;

import jakarta.validation.constraints.NotNull;

/** Body del POST /api/garita/ingreso (US-39). El barrioId y guardiaId vienen del JWT. */
public record IngresoRequest(
        @NotNull(message = "El trabajador es obligatorio") Long trabajadorId
) {}
