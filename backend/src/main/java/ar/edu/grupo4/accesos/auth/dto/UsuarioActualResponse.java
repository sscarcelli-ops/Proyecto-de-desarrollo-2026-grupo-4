package ar.edu.grupo4.accesos.auth.dto;

import ar.edu.grupo4.accesos.usuario.Usuario;

import java.util.List;

/** El usuario de la sesión (GET /api/auth/me): mismos campos que CurrentUser del frontend. */
public record UsuarioActualResponse(String email, List<String> roles) {

    public static UsuarioActualResponse from(Usuario usuario) {
        return new UsuarioActualResponse(usuario.getEmail(), usuario.codigosDeRoles());
    }
}
