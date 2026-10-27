package ar.edu.grupo4.accesos.perfil.dto;

import ar.edu.grupo4.accesos.perfil.EstadoPerfil;
import ar.edu.grupo4.accesos.perfil.Perfil;
import ar.edu.grupo4.accesos.perfil.TipoPerfil;

import java.time.OffsetDateTime;

public record PerfilResponse(
    Long id,
    Long usuarioId,
    TipoPerfil tipo,
    EstadoPerfil estado,
    String cuitCuil,
    String razonSocial,
    String nombre,
    String apellido,
    String dniTitular,
    String telefono,
    String constanciaRuta,
    String motivoRechazo,
    OffsetDateTime creadoEn
) {
    public static PerfilResponse from(Perfil p) {
        return new PerfilResponse(
            p.getId(),
            p.getUsuario().getId(),
            p.getTipo(),
            p.getEstado(),
            p.getCuitCuil(),
            p.getRazonSocial(),
            p.getNombre(),
            p.getApellido(),
            p.getDniTitular(),
            p.getTelefono(),
            p.getConstanciaRuta(),
            p.getMotivoRechazo(),
            p.getCreadoEn()
        );
    }
}
