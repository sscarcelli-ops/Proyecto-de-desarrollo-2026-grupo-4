package ar.edu.grupo4.accesos.perfil.dto;

import ar.edu.grupo4.accesos.perfil.TipoPerfil;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * US-03: solicitud de perfil de Empresa o Monotributista.
 */
public record SolicitarPerfilRequest(
    @NotNull(message = "El tipo de perfil es obligatorio")
    TipoPerfil tipo,

    String cuit,          // Empresa
    String razonSocial,   // Empresa

    String cuil,          // Monotributista
    String nombre,        // Monotributista
    String apellido,      // Monotributista
    String dni,           // Monotributista

    String telefono,      // Opcional en ambos

    MultipartFile constancia // Obligatorio, PDF hasta 8 MB
) {
}
