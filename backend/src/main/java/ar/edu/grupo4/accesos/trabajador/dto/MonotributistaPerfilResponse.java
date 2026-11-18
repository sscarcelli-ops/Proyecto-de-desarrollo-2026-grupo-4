package ar.edu.grupo4.accesos.trabajador.dto;

import ar.edu.grupo4.accesos.perfil.Perfil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Respuesta completa del perfil propio del Monotributista (US-24, RF-09, RN-07).
 *
 * Integra los datos fiscales de la entidad Perfil (CUIL, teléfono) con los datos
 * operativos de su registro automático en Trabajador (foto, estado documental, ART, Seguro).
 */
public record MonotributistaPerfilResponse(
    Long perfilId,
    Long trabajadorId,
    String nombre,
    String apellido,
    String dni,
    String cuil,
    @Schema(types = {"string", "null"})
    String telefono,
    String estadoDocumental,
    boolean tieneFoto,
    @Schema(types = {"string", "null"})
    String fotoUrl,
    List<DocumentoResponse> documentos
) {
    public static MonotributistaPerfilResponse from(Perfil perfil, TrabajadorResponse trabajador) {
        return new MonotributistaPerfilResponse(
            perfil.getId(),
            trabajador.id(),
            trabajador.nombre(),
            trabajador.apellido(),
            trabajador.dni(),
            perfil.getCuitCuil(),
            perfil.getTelefono(),
            trabajador.estadoDocumental(),
            trabajador.tieneFoto(),
            trabajador.tieneFoto() ? "/api/monotributista/perfil/foto" : null,
            trabajador.documentos()
        );
    }
}
