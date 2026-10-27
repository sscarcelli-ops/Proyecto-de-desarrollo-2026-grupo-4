package ar.edu.grupo4.accesos.auth.dto;

import ar.edu.grupo4.accesos.common.security.TokenService.TokenEmitido;
import ar.edu.grupo4.accesos.usuario.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Respuesta del login: el token para el header Authorization: Bearer y el usuario (mismos campos que
 * CurrentUser del frontend). expiraEn = cuándo vence el token (ISO-8601, UTC).
 * nombre: null para el Administrador o una cuenta sin perfil.
 */
public record LoginResponse(
    String token,
    Instant expiraEn,
    String email,
    @Schema(types = {"string", "null"}) String nombre,
    List<String> roles
) {

    public static LoginResponse from(TokenEmitido token, Usuario usuario, String nombre) {
        return new LoginResponse(token.token(), token.expiraEn(), usuario.getEmail(), nombre,
            usuario.codigosDeRoles());
    }
}
