package ar.edu.grupo4.accesos.barrio.dto;

import jakarta.validation.Valid;

/**
 * US-45: asignar un Operador a un barrio sin Operador. Exactamente uno de los dos:
 * operadorId (un Operador disponible) o nuevoOperador (se crea y se asigna en la misma operación).
 */
public record AsignarOperadorRequest(
    Long operadorId,
    @Valid OperadorRequest nuevoOperador
) {}
