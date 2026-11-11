package ar.edu.grupo4.accesos.perfil;

import ar.edu.grupo4.accesos.common.paging.OrdenPermitido;
import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.perfil.dto.PerfilBarrioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * US-33 y US-34: Empresas y Monotributistas aprobados en el barrio del Operador.
 * "barrio" en singular: siempre es el barrio de la sesión, nunca uno que venga en la URL (US-42).
 */
@RestController
@RequestMapping("/api/barrio/perfiles")
@PreAuthorize("hasRole('OPERADOR')")
@RequiredArgsConstructor
public class HabilitacionBarrioController {

    private final HabilitacionBarrioService habilitacionBarrioService;

    /** US-33: listado paginado de perfiles aprobados, con su habilitación en el barrio. */
    @GetMapping
    public PageResponse<PerfilBarrioResponse> listar(@PageableDefault(size = 10) Pageable pageable) {
        // El orden lo fija la consulta (alfabético, RNF-08): no se acepta ?sort=
        return habilitacionBarrioService.listar(OrdenPermitido.validar(pageable, Set.of()));
    }

    /** US-34: habilita el perfil en el barrio. */
    @PostMapping("/{id}/habilitar")
    public PerfilBarrioResponse habilitar(@PathVariable Long id) {
        return habilitacionBarrioService.habilitar(id);
    }

    /** US-34: deshabilita el perfil en el barrio. */
    @PostMapping("/{id}/deshabilitar")
    public PerfilBarrioResponse deshabilitar(@PathVariable Long id) {
        return habilitacionBarrioService.deshabilitar(id);
    }
}
