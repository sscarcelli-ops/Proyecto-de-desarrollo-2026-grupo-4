package ar.edu.grupo4.accesos.perfil;

import ar.edu.grupo4.accesos.perfil.dto.PerfilResponse;
import ar.edu.grupo4.accesos.perfil.dto.SolicitarPerfilRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final PerfilService perfilService;

    /**
     * US-03: Paso 2 del registro. Declarar perfil fiscal de Empresa o Monotributista.
     * Requiere sesión activa (JWT del paso 1 de US-02).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PerfilResponse solicitar(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @ModelAttribute SolicitarPerfilRequest request
    ) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        return perfilService.solicitar(usuarioId, request);
    }

    /**
     * Consulta el perfil asociado a la cuenta en sesión (para saber si tiene solicitud y en qué estado).
     */
    @GetMapping("/mi-solicitud")
    public PerfilResponse miSolicitud(@AuthenticationPrincipal Jwt jwt) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        return perfilService.obtenerPorUsuarioId(usuarioId);
    }
}
