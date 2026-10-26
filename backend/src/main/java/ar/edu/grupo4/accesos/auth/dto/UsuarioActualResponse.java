package ar.edu.grupo4.accesos.auth.dto;

import ar.edu.grupo4.accesos.usuario.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * El usuario de la sesión (GET /api/auth/me): mismos campos que CurrentUser del frontend.
 * nombre: "Carlos Gómez", la razón social de una Empresa, etc.; null para el Administrador o sin perfil.
 */
public record UsuarioActualResponse(
    String email,
    @Schema(types = {"string", "null"}) String nombre,
    List<String> roles
) {

    public static UsuarioActualResponse from(Usuario usuario, String nombre) {
        return new UsuarioActualResponse(usuario.getEmail(), nombre, usuario.codigosDeRoles());
    }
}
