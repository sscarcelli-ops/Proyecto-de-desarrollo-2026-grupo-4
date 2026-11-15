package ar.edu.grupo4.accesos.movimiento;

import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.movimiento.dto.BarrioFiltroResponse;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoPropioFiltros;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoPropioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints para consultar el historial de accesos propio (US-31 para Empresa, US-46 para Monotributista).
 */
@RestController
@RequestMapping("/api/propio/movimientos")
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoService movimientoService;

    /**
     * Consulta paginada del historial de accesos de la Empresa o Monotributista autenticado.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPRESA', 'MONOTRIBUTISTA')")
    public PageResponse<MovimientoPropioResponse> consultarHistorialPropio(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) Long barrioId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        MovimientoPropioFiltros filtros = new MovimientoPropioFiltros(desde, hasta, dni, barrioId);
        return movimientoService.consultarHistorialPropio(filtros, pageable);
    }

    /**
     * Barrios donde el perfil autenticado está habilitado, para el filtro de barrio (US-31).
     */
    @GetMapping("/barrios")
    @PreAuthorize("hasAnyRole('EMPRESA', 'MONOTRIBUTISTA')")
    public List<BarrioFiltroResponse> listarBarriosHabilitados() {
        return movimientoService.listarBarriosHabilitados();
    }
}
