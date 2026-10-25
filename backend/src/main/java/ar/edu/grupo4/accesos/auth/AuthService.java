package ar.edu.grupo4.accesos.auth;

import ar.edu.grupo4.accesos.auth.dto.LoginRequest;
import ar.edu.grupo4.accesos.auth.dto.LoginResponse;
import ar.edu.grupo4.accesos.auth.dto.UsuarioActualResponse;
import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.security.TokenService;
import ar.edu.grupo4.accesos.usuario.Usuario;
import ar.edu.grupo4.accesos.usuario.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    // Mismo mensaje si el email no existe o si la contraseña no coincide: no revela qué cuentas existen
    private static final String CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos";

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    // Hash de relleno: si el email no existe se compara igual, así la respuesta tarda lo mismo
    // y no se puede adivinar qué emails están registrados midiendo el tiempo
    private final String hashDeRelleno;

    public AuthService(UsuarioService usuarioService, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.hashDeRelleno = passwordEncoder.encode("relleno-para-emails-inexistentes");
    }

    /** US-04: valida email y contraseña y emite el token de sesión. */
    public LoginResponse login(LoginRequest request) {
        Optional<Usuario> usuario = usuarioService.buscarPorEmail(request.email());
        String hash = usuario.map(Usuario::getPasswordHash).orElse(hashDeRelleno);

        if (!passwordEncoder.matches(request.password(), hash) || usuario.isEmpty()) {
            throw new BusinessException("CREDENCIALES_INVALIDAS", CREDENCIALES_INVALIDAS, HttpStatus.UNAUTHORIZED);
        }
        return LoginResponse.from(tokenService.emitir(usuario.get()), usuario.get());
    }

    /** El usuario del token (sub = id). Si la cuenta ya no existe, la sesión deja de valer (401). */
    public UsuarioActualResponse usuarioActual(Long usuarioId) {
        return usuarioService.buscarPorId(usuarioId)
            .map(UsuarioActualResponse::from)
            .orElseThrow(() -> new InsufficientAuthenticationException("La cuenta del token no existe"));
    }
}
