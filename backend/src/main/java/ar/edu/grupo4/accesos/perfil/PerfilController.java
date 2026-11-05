package ar.edu.grupo4.accesos.perfil;

import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.perfil.dto.PerfilResponse;
import ar.edu.grupo4.accesos.perfil.dto.RechazarPerfilRequest;
import ar.edu.grupo4.accesos.perfil.dto.SolicitarPerfilRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PerfilController {

    private final PerfilService perfilService;

    /**
     * US-03: Paso 2 del registro. Declarar perfil fiscal de Empresa o Monotributista.
     * Requiere sesión activa (JWT del paso 1 de US-02).
     */
    @PostMapping(value = "/perfil", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PerfilResponse solicitar(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @ModelAttribute SolicitarPerfilRequest request
    ) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        return perfilService.solicitar(usuarioId, request);
    }

    /**
     * US-08: Consulta el perfil asociado a la cuenta en sesión (para saber si tiene solicitud y en qué estado).
     */
    @GetMapping("/perfil/mi-solicitud")
    public PerfilResponse miSolicitud(@AuthenticationPrincipal Jwt jwt) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        return perfilService.obtenerPorUsuarioId(usuarioId);
    }

    /**
     * US-18: Listado paginado de Empresas y Monotributistas con filtros opcionales de estado y búsqueda.
     * Solo accesible por el Administrador de la plataforma.
     */
    @GetMapping("/perfiles")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<PerfilResponse> listar(
        @RequestParam(required = false) EstadoPerfil estado,
        @RequestParam(required = false) String q,
        @PageableDefault(size = 10) Pageable pageable
    ) {
        return perfilService.listar(estado, q, pageable);
    }

    /**
     * US-18: Detalle de un perfil específico para el Administrador.
     */
    @GetMapping("/perfiles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PerfilResponse obtenerPorId(@PathVariable Long id) {
        return perfilService.obtenerPorId(id);
    }

    /**
     * US-18: Descarga o visualización de la constancia de inscripción en PDF.
     * Solo accesible por el Administrador.
     */
    @GetMapping("/perfiles/{id}/constancia")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> obtenerConstancia(@PathVariable Long id) {
        Resource resource = perfilService.obtenerConstancia(id);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"constancia-" + id + ".pdf\"")
            .body(resource);
    }

    /**
     * US-20: Aprueba la solicitud de una Empresa o Monotributista.
     * Asigna el rol correspondiente y deja el perfil deshabilitado en todos los barrios.
     */
    @PostMapping("/perfiles/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public PerfilResponse aprobar(@PathVariable Long id) {
        return perfilService.aprobar(id);
    }

    /**
     * US-20: Rechaza la solicitud de una Empresa o Monotributista con motivo obligatorio.
     */
    @PostMapping("/perfiles/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    public PerfilResponse rechazar(
        @PathVariable Long id,
        @Valid @RequestBody RechazarPerfilRequest request
    ) {
        return perfilService.rechazar(id, request);
    }
}
