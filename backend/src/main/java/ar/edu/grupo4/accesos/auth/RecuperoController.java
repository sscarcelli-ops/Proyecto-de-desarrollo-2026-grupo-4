package ar.edu.grupo4.accesos.auth;

import ar.edu.grupo4.accesos.auth.dto.RestablecerPasswordRequest;
import ar.edu.grupo4.accesos.auth.dto.SolicitarRecuperoRequest;
import ar.edu.grupo4.accesos.auth.dto.ValidarRecuperoRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-06: recuperar la contraseña. Las tres rutas son públicas (SecurityConfig): la persona no puede iniciar sesión.
 * El código viaja en el cuerpo y no en la URL de la API, para que no quede en los registros de los servidores.
 */
@RestController
@RequestMapping("/api/auth/recupero")
@RequiredArgsConstructor
public class RecuperoController {

    private final RecuperoService recuperoService;

    /** CA-1 y CA-2: siempre 204, exista o no el email. */
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void solicitarRecupero(@Valid @RequestBody SolicitarRecuperoRequest request) {
        recuperoService.solicitar(request.email());
    }

    /** Al abrir el enlace: 204 si sirve, 422 si no existe, se usó o venció (CA-4). No lo consume. */
    @PostMapping("/validar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validarEnlaceRecupero(@Valid @RequestBody ValidarRecuperoRequest request) {
        recuperoService.validar(request.token());
    }

    /** CA-3: define la contraseña nueva y consume el enlace. */
    @PostMapping("/restablecer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest request) {
        recuperoService.restablecer(request.token(), request.password());
    }
}
