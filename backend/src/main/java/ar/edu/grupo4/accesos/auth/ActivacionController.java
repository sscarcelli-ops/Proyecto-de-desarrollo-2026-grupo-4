package ar.edu.grupo4.accesos.auth;

import ar.edu.grupo4.accesos.auth.dto.ActivacionRequest;
import ar.edu.grupo4.accesos.auth.dto.ValidarActivacionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-01: activación del Administrador inicial. Públicos (RUTAS_PUBLICAS): el Administrador todavía no tiene cuenta.
 * Errores: TOKEN_INVALIDO (no existe o venció), TOKEN_UTILIZADO, EMAIL_DUPLICADO y VALIDATION_ERROR.
 * Con el Administrador ya creado, todos responden 404, como una ruta inexistente (ActivacionCerradaInterceptor).
 */
@RestController
@RequestMapping("/api/auth/activacion")
@RequiredArgsConstructor
public class ActivacionController {

    private final ActivacionService activacionService;

    // La pantalla /activar pregunta antes de mostrar el campo del código: 204 si la activación sigue abierta y 404
    // (ActivacionCerradaInterceptor) si ya hay Administrador, y entonces muestra "página no encontrada"
    @GetMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disponible() {
        // Sin cuerpo: la respuesta la decide el interceptor
    }

    // "Activación de cuenta": 204 si el código sirve para pasar a "Registrate"
    @PostMapping("/validar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validar(@Valid @RequestBody ValidarActivacionRequest request) {
        activacionService.validar(request.token());
    }

    // "Registrate": crea el Administrador y consume el código. Sin token de sesión: después inicia sesión en el login
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void activar(@Valid @RequestBody ActivacionRequest request) {
        activacionService.activar(request);
    }
}
