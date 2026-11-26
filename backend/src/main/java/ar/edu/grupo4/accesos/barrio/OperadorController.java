package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.OperadorListadoResponse;
import ar.edu.grupo4.accesos.barrio.dto.OperadorModificarRequest;
import ar.edu.grupo4.accesos.barrio.dto.OperadorRequest;
import ar.edu.grupo4.accesos.barrio.dto.OperadorResponse;
import ar.edu.grupo4.accesos.common.paging.OrdenPermitido;
import ar.edu.grupo4.accesos.common.paging.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

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

    // US-15: listado paginado de Operadores con su barrio. El orden lo fija la consulta (alfabético, RNF-08): no se
    // acepta ?sort=
    @GetMapping
    public PageResponse<OperadorListadoResponse> listar(
        @RequestParam(required = false) EstadoOperador estado,
        @RequestParam(required = false) @Size(max = 100, message = "La búsqueda no puede superar los 100 caracteres") String q,
        @PageableDefault(size = 10) Pageable pageable
    ) {
        return operadorService.listar(estado, q, OrdenPermitido.validar(pageable, Set.of()));
    }

    // US-16: datos de un Operador, para abrir el formulario de edición (el email se muestra y no se edita)
    @GetMapping("/{id}")
    public OperadorResponse obtener(@PathVariable Long id) {
        return operadorService.obtener(id);
    }

    // US-16: modifica nombre, apellido y teléfono (RF-32). El email no se puede cambiar.
    @PutMapping("/{id}")
    public OperadorResponse modificar(@PathVariable Long id, @Valid @RequestBody OperadorModificarRequest request) {
        return operadorService.modificar(id, request);
    }
}
