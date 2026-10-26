package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.BarrioRequest;
import ar.edu.grupo4.accesos.barrio.dto.BarrioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/barrios")
@RequiredArgsConstructor
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
}
