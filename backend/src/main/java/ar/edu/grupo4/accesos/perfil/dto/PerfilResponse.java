package ar.edu.grupo4.accesos.perfil.dto;

import ar.edu.grupo4.accesos.perfil.EstadoPerfil;
import ar.edu.grupo4.accesos.perfil.Perfil;
import ar.edu.grupo4.accesos.perfil.TipoPerfil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

public record PerfilResponse(
    Long id,
    Long usuarioId,
    @Schema(types = {"string", "null"}) String email,
    TipoPerfil tipo,
    EstadoPerfil estado,
    String cuitCuil,
    @Schema(types = {"string", "null"}) String razonSocial,
    @Schema(types = {"string", "null"}) String nombre,
    @Schema(types = {"string", "null"}) String apellido,
    @Schema(types = {"string", "null"}) String dniTitular,
    @Schema(types = {"string", "null"}) String telefono,
    @Schema(types = {"string", "null"}) String motivoRechazo,
    OffsetDateTime creadoEn
) {
    public static PerfilResponse from(Perfil p) {
        String email = p.getUsuario() != null ? p.getUsuario().getEmail() : null;
        Long usuarioId = p.getUsuario() != null ? p.getUsuario().getId() : null;
        return new PerfilResponse(
            p.getId(),
            usuarioId,
            email,
            p.getTipo(),
            p.getEstado(),
            p.getCuitCuil(),
            p.getRazonSocial(),
            p.getNombre(),
            p.getApellido(),
            p.getDniTitular(),
            p.getTelefono(),
            p.getMotivoRechazo(),
            p.getCreadoEn()
        );
    }
}
