package ar.edu.grupo4.accesos.auth;

import ar.edu.grupo4.accesos.auth.dto.LoginRequest;
import ar.edu.grupo4.accesos.auth.dto.LoginResponse;
import ar.edu.grupo4.accesos.auth.dto.RegistroRequest;
import ar.edu.grupo4.accesos.auth.dto.UsuarioActualResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    // US-02: Público (SecurityConfig). Registro paso 1, crea usuario e inicia sesión
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse registrar(@Valid @RequestBody RegistroRequest request) {
        return authService.registrar(request);
    }

    // Con token: quién es el usuario de la sesión. El frontend lo usa al recargar la página (useCurrentUser)
    @GetMapping("/me")
    public UsuarioActualResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.usuarioActual(Long.valueOf(jwt.getSubject()));
    }
}
