package ar.edu.grupo4.accesos.trabajador;

import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorEditarRequest;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorRequest;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST para la gestión de trabajadores (US-25, US-26).
 *
 * Seguridad:
 * - Todos los endpoints requieren un JWT válido (SecurityConfig exige token para /api/**).
 * - @PreAuthorize restringe el acceso a los roles EMPRESA y MONOTRIBUTISTA.
 *   Otros roles (ADMIN, GUARDIA, OPERADOR) recibirán HTTP 403.
 * - El perfilId NUNCA se recibe del frontend: se resuelve internamente desde el JWT
 *   (vía PerfilIdResolver), garantizando aislamiento entre empresas (RF-23).
 *
 * Paginación:
 * - Por defecto se muestran 10 trabajadores por página, ordenados por apellido (A-Z).
 * - El frontend puede cambiar página, tamaño y orden con query params estándar de Spring:
 *   ?page=0&size=10&sort=apellido,asc
 */
@RestController
@RequestMapping("/api/trabajadores")
@PreAuthorize("hasRole('EMPRESA') or hasRole('MONOTRIBUTISTA')")
@RequiredArgsConstructor
public class TrabajadorController {

    private final TrabajadorService trabajadorService;

    // ========================================================================================
    // US-25: Registrar trabajador
    // ========================================================================================

    /**
     * Crea un nuevo trabajador para el perfil del usuario autenticado.
     * En S1 solo se envían nombre, apellido y DNI (sin archivos).
     *
     * @param jwt   token del usuario autenticado (inyectado por Spring Security)
     * @param request datos del trabajador validados por Bean Validation
     * @return el trabajador creado con su estado documental calculado
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201: recurso creado correctamente
    public TrabajadorResponse crear(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TrabajadorRequest request) {

        return trabajadorService.crear(usuarioId(jwt), request);
    }

    // ========================================================================================
    // US-26: Listar y modificar trabajadores
    // ========================================================================================

    /**
     * Lista paginada de trabajadores del perfil del usuario autenticado.
     * Spring traduce automáticamente los query params (?page, ?size, ?sort) al objeto Pageable.
     *
     * @PageableDefault define los valores si el frontend no envía ninguno:
     *   - size = 10 (10 filas por página, coincide con el wireframe)
     *   - sort = "apellido" (orden alfabético por defecto)
     */
    @GetMapping
    public PageResponse<TrabajadorResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10, sort = "apellido") Pageable pageable) {

        return trabajadorService.listar(usuarioId(jwt), pageable);
    }

    /**
     * Detalle de un trabajador específico.
     * Si el trabajador no existe o pertenece a otro perfil → 404 (no 403,
     * para no revelar la existencia de trabajadores ajenos).
     */
    @GetMapping("/{id}")
    public TrabajadorResponse obtener(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        return trabajadorService.obtener(usuarioId(jwt), id);
    }

    /**
     * Modifica nombre y/o apellido de un trabajador existente.
     * El DNI no se puede cambiar una vez registrado (viene así del requerimiento).
     * La foto y documentos se modifican aparte (US-28, S2).
     */
    @PutMapping("/{id}")
    public TrabajadorResponse editar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody TrabajadorEditarRequest request) {

        return trabajadorService.editar(usuarioId(jwt), id, request);
    }

    // ========================================================================================
    // Helper privado
    // ========================================================================================

    /**
     * Extrae el ID del usuario del claim "sub" del JWT.
     * Este es el patrón establecido en AuthController.me() por Persona 1.
     */
    private static Long usuarioId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
