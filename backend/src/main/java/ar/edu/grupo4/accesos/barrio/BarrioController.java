package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.AsignarOperadorRequest;
import ar.edu.grupo4.accesos.barrio.dto.BarrioRequest;
import ar.edu.grupo4.accesos.barrio.dto.BarrioResponse;
import ar.edu.grupo4.accesos.barrio.dto.EstadoBarrioRequest;
import ar.edu.grupo4.accesos.common.paging.OrdenPermitido;
import ar.edu.grupo4.accesos.common.paging.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/barrios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BarrioController {

    private final BarrioService barrioService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BarrioResponse crear(@Valid @RequestBody BarrioRequest request) {
        return responder(barrioService.crear(request));
    }

    // US-10 y US-45: listado paginado (RNF-08) con el selector de estado (?filtro=ACTIVO | INACTIVO | SIN_OPERADOR).
    // El orden es fijo (alfabético): no se acepta ?sort=.
    @GetMapping
    public PageResponse<BarrioResponse> listar(
        @RequestParam(required = false) FiltroBarrio filtro,
        @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<Barrio> pagina = barrioService.listar(filtro, OrdenPermitido.validar(pageable, Set.of()));
        // Los Operadores de la página en una sola consulta
        Map<Long, Operador> operadores = barrioService.operadoresDe(pagina.getContent());
        return PageResponse.from(pagina, b -> BarrioResponse.from(b, operadorDe(b, operadores)));
    }

    // US-11
    @GetMapping("/{id}")
    public BarrioResponse obtener(@PathVariable Long id) {
        return responder(barrioService.obtener(id));
    }

    // US-11
    @PutMapping("/{id}")
    public BarrioResponse modificar(@PathVariable Long id, @Valid @RequestBody BarrioRequest request) {
        return responder(barrioService.modificar(id, request));
    }

    // US-45: asigna un Operador (disponible o nuevo) al barrio y lo activa
    @PutMapping("/{id}/operador")
    public BarrioResponse asignarOperador(@PathVariable Long id, @Valid @RequestBody AsignarOperadorRequest request) {
        return responder(barrioService.asignarOperador(id, request));
    }

    // US-14: reemplaza al Operador del barrio (activo o inactivo) por uno disponible o uno nuevo, en una sola operación
    @PutMapping("/{id}/operador/reemplazo")
    public BarrioResponse reemplazarOperador(@PathVariable Long id, @Valid @RequestBody AsignarOperadorRequest request) {
        return responder(barrioService.reemplazarOperador(id, request));
    }

    // US-12: activa o desactiva el barrio (baja lógica). Al desactivar registra el egreso de los ingresos abiertos
    @PutMapping("/{id}/estado")
    public BarrioResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoBarrioRequest request) {
        return responder(barrioService.cambiarEstado(id, request.activo()));
    }

    private BarrioResponse responder(Barrio barrio) {
        return BarrioResponse.from(barrio, operadorDe(barrio, barrioService.operadoresDe(List.of(barrio))));
    }

    private static Operador operadorDe(Barrio barrio, Map<Long, Operador> operadores) {
        return barrio.getOperadorId() == null ? null : operadores.get(barrio.getOperadorId());
    }
}
