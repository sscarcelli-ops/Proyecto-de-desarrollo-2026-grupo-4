package ar.edu.grupo4.accesos.movimiento;

import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.movimiento.dto.EntidadFiltroResponse;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoFiltros;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoHistorialResponse;
import ar.edu.grupo4.accesos.usuario.UsuarioActual;
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
 * US-36: Consultar el historial de accesos del barrio (Operador).
 */
@RestController
@RequestMapping("/api/barrio/movimientos")
@PreAuthorize("hasRole('OPERADOR')")
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoService movimientoService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public PageResponse<MovimientoHistorialResponse> consultarHistorial(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) Long perfilId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Long barrioId = usuarioActual.barrioId();
        MovimientoFiltros filtros = new MovimientoFiltros(desde, hasta, dni, perfilId);
        return movimientoService.consultarHistorial(barrioId, filtros, pageable);
    }

    @GetMapping("/entidades")
    public List<EntidadFiltroResponse> listarEntidadesFiltro() {
        Long barrioId = usuarioActual.barrioId();
        return movimientoService.listarEntidadesFiltro(barrioId);
    }
}
