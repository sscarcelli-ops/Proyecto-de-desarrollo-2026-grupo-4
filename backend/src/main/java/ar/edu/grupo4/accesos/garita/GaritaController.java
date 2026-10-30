package ar.edu.grupo4.accesos.garita;

import ar.edu.grupo4.accesos.garita.dto.BusquedaDniResponse;
import ar.edu.grupo4.accesos.garita.dto.EgresoRequest;
import ar.edu.grupo4.accesos.garita.dto.IngresoRequest;
import ar.edu.grupo4.accesos.garita.dto.MovimientoResponse;
import ar.edu.grupo4.accesos.garita.dto.VerificacionRequest;
import ar.edu.grupo4.accesos.garita.dto.VerificacionResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de la Garita (US-37, US-38, US-39, US-40).
 * Todos requieren rol GUARDIA.
 *
 * <ul>
 *   <li>GET  /api/garita/buscar?dni=30111222   – Buscar trabajador por DNI</li>
 *   <li>POST /api/garita/verificar             – Verificar acceso antes de ingresar</li>
 *   <li>POST /api/garita/ingreso               – Registrar ingreso</li>
 *   <li>POST /api/garita/egreso                – Registrar egreso</li>
 * </ul>
 *
 * {@code barrioId} y {@code guardiaId} se extraen siempre del JWT ({@code sub} = usuarioId del guardia).
 */
@RestController
@RequestMapping("/api/garita")
@PreAuthorize("hasRole('GUARDIA')")
@RequiredArgsConstructor
public class GaritaController {

    private final GaritaService garitaService;
    private final GaritaRepository garitaRepository;

    // ──────────────────────────────────────────────────────────────────
    // US-37 – Búsqueda por DNI
    // ──────────────────────────────────────────────────────────────────

    /**
     * GET /api/garita/buscar?dni=30111222
     * Retorna los vínculos laborales del trabajador e indica si hay un ingreso activo en el barrio.
     */
    @GetMapping("/buscar")
    public BusquedaDniResponse buscar(
            @RequestParam @NotBlank String dni,
            @AuthenticationPrincipal Jwt jwt) {

        Long guardiaId = Long.valueOf(jwt.getSubject());
        Long barrioId = garitaRepository.obtenerBarrioDelGuardia(guardiaId);

        return garitaService.buscarPorDni(dni, barrioId);
    }

    // ──────────────────────────────────────────────────────────────────
    // US-38 – Verificación de acceso
    // ──────────────────────────────────────────────────────────────────

    /**
     * POST /api/garita/verificar
     * Invoca la función PostgreSQL verificar_acceso y retorna habilitado + motivo.
     */
    @PostMapping("/verificar")
    public VerificacionResponse verificar(
            @Valid @RequestBody VerificacionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Long guardiaId = Long.valueOf(jwt.getSubject());
        Long barrioId = garitaRepository.obtenerBarrioDelGuardia(guardiaId);

        return garitaService.verificarAcceso(request.trabajadorId(), barrioId);
    }

    // ──────────────────────────────────────────────────────────────────
    // US-39 – Registrar ingreso
    // ──────────────────────────────────────────────────────────────────

    /**
     * POST /api/garita/ingreso
     * Re-verifica y registra el ingreso. Devuelve 422 si la habilitación falló.
     */
    @PostMapping("/ingreso")
    public MovimientoResponse ingreso(
            @Valid @RequestBody IngresoRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Long guardiaId = Long.valueOf(jwt.getSubject());
        Long barrioId = garitaRepository.obtenerBarrioDelGuardia(guardiaId);

        return garitaService.registrarIngreso(request.trabajadorId(), barrioId, guardiaId);
    }

    // ──────────────────────────────────────────────────────────────────
    // US-40 – Registrar egreso
    // ──────────────────────────────────────────────────────────────────

    /**
     * POST /api/garita/egreso
     * Busca el ingreso activo del trabajador en el barrio y registra el egreso.
     */
    @PostMapping("/egreso")
    public MovimientoResponse egreso(
            @Valid @RequestBody EgresoRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Long guardiaId = Long.valueOf(jwt.getSubject());
        Long barrioId = garitaRepository.obtenerBarrioDelGuardia(guardiaId);

        return garitaService.registrarEgreso(request.trabajadorId(), barrioId, guardiaId);
    }
}
