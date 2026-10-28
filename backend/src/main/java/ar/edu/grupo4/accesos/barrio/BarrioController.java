package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.BarrioRequest;
import ar.edu.grupo4.accesos.barrio.dto.BarrioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/barrios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BarrioController {

    private final BarrioService barrioService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BarrioResponse crear(@Valid @RequestBody BarrioRequest request) {
        return BarrioResponse.from(barrioService.crear(request));
    }

    @GetMapping
    public List<BarrioResponse> listar() {
        return barrioService.listar().stream().map(BarrioResponse::from).toList();
    }

    // US-11
    @GetMapping("/{id}")
    public BarrioResponse obtener(@PathVariable Long id) {
        return BarrioResponse.from(barrioService.obtener(id));
    }

    // US-11
    @PutMapping("/{id}")
    public BarrioResponse modificar(@PathVariable Long id, @Valid @RequestBody BarrioRequest request) {
        return BarrioResponse.from(barrioService.modificar(id, request));
    }
}
