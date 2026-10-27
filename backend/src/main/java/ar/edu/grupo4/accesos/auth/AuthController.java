package ar.edu.grupo4.accesos.auth;

import ar.edu.grupo4.accesos.auth.dto.LoginRequest;
import ar.edu.grupo4.accesos.auth.dto.LoginResponse;
import ar.edu.grupo4.accesos.auth.dto.UsuarioActualResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Público (SecurityConfig). Los errores (credenciales inválidas, campos vacíos) los arma GlobalExceptionHandler
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // Con token: quién es el usuario de la sesión. El frontend lo usa al recargar la página (useCurrentUser)
    @GetMapping("/me")
    public UsuarioActualResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.usuarioActual(Long.valueOf(jwt.getSubject()));
    }
}
