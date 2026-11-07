package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.AsignarOperadorRequest;
import ar.edu.grupo4.accesos.barrio.dto.BarrioRequest;
import ar.edu.grupo4.accesos.barrio.dto.BarrioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping
    public List<BarrioResponse> listar() {
        List<Barrio> barrios = barrioService.listar();
        Map<Long, Operador> operadores = barrioService.operadoresDe(barrios);
        return barrios.stream().map(b -> BarrioResponse.from(b, operadorDe(b, operadores))).toList();
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

    private BarrioResponse responder(Barrio barrio) {
        return BarrioResponse.from(barrio, operadorDe(barrio, barrioService.operadoresDe(List.of(barrio))));
    }

    private static Operador operadorDe(Barrio barrio, Map<Long, Operador> operadores) {
        return barrio.getOperadorId() == null ? null : operadores.get(barrio.getOperadorId());
    }
}
