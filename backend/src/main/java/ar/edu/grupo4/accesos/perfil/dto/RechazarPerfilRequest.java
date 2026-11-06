package ar.edu.grupo4.accesos.perfil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * US-20: Petición para rechazar una solicitud de perfil de Empresa o Monotributista.
 * El motivo es obligatorio para fundamentar el rechazo ante el usuario solicitante.
 */
public record RechazarPerfilRequest(
    @NotBlank(message = "El motivo de rechazo es obligatorio")
    @Size(max = 500, message = "El motivo de rechazo no puede superar los 500 caracteres")
    String motivo
) {}
