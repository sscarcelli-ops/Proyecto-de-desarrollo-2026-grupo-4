package ar.edu.grupo4.accesos.guardia;

import ar.edu.grupo4.accesos.guardia.dto.EstadoGuardiaRequest;
import ar.edu.grupo4.accesos.guardia.dto.GuardiaModificarRequest;
import ar.edu.grupo4.accesos.guardia.dto.GuardiaRequest;
import ar.edu.grupo4.accesos.guardia.dto.GuardiaResponse;
import ar.edu.grupo4.accesos.common.paging.OrdenPermitido;
import ar.edu.grupo4.accesos.common.paging.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/** US-32: Guardias del barrio del Operador (RF-14). */
@RestController
@RequestMapping("/api/guardias")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OPERADOR')")
public class GuardiaController {

    private final GuardiaService guardiaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuardiaResponse crear(@Valid @RequestBody GuardiaRequest request) {
        return guardiaService.crear(request);
    }

    // Paginado (RNF-08). El orden lo fija la consulta (apellido y nombre): no se acepta ?sort=
    @GetMapping
    public PageResponse<GuardiaResponse> listar(@PageableDefault(size = 10) Pageable pageable) {
        return guardiaService.listar(OrdenPermitido.validar(pageable, Set.of()));
    }

    @GetMapping("/{id}")
    public GuardiaResponse obtener(@PathVariable Long id) {
        return guardiaService.obtener(id);
    }

    @PutMapping("/{id}")
    public GuardiaResponse modificar(@PathVariable Long id, @Valid @RequestBody GuardiaModificarRequest request) {
        return guardiaService.modificar(id, request);
    }

    @PutMapping("/{id}/estado")
    public GuardiaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoGuardiaRequest request) {
        return guardiaService.cambiarEstado(id, request.habilitado());
    }
}
