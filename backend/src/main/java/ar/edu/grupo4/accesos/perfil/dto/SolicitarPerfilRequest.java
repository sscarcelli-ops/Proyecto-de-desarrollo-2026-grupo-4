package ar.edu.grupo4.accesos.perfil.dto;

import ar.edu.grupo4.accesos.perfil.TipoPerfil;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/**
 * US-03: solicitud de perfil de Empresa o Monotributista.
 */
public record SolicitarPerfilRequest(
    @NotNull(message = "El tipo de perfil es obligatorio")
    TipoPerfil tipo,

    @Size(max = 13, message = "El CUIT no puede superar los 13 caracteres")
    String cuit,          // Empresa

    @Size(max = 200, message = "La razón social no puede superar los 200 caracteres")
    String razonSocial,   // Empresa

    @Size(max = 13, message = "El CUIL no puede superar los 13 caracteres")
    String cuil,          // Monotributista

    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    String nombre,        // Monotributista

    @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
    String apellido,      // Monotributista

    @Size(max = 10, message = "El DNI no puede superar los 10 caracteres")
    String dni,           // Monotributista

    @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
    String telefono,      // Opcional en ambos

    MultipartFile constancia // Obligatorio, PDF hasta 8 MB
) {
}
