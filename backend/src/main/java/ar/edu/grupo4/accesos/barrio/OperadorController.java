package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.OperadorRequest;
import ar.edu.grupo4.accesos.barrio.dto.OperadorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/operadores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class OperadorController {

    private final OperadorService operadorService;

    // US-13
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OperadorResponse crear(@Valid @RequestBody OperadorRequest request) {
        return operadorService.crear(request);
    }

    // US-45: Operadores sin barrio, para el selector de "Asignar Operador"
    @GetMapping("/disponibles")
    public List<OperadorResponse> disponibles() {
        return operadorService.listarDisponibles();
    }
}
