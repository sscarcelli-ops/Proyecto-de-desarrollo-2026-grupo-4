package ar.edu.grupo4.accesos.perfil;

import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.perfil.dto.PerfilBarrioResponse;
import ar.edu.grupo4.accesos.usuario.UsuarioActual;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * US-33 y US-34: el Operador ve las Empresas y Monotributistas aprobados y los habilita o deshabilita en su barrio.
 * El barrio sale siempre de la sesión (US-42): el Operador no puede consultar ni cambiar otro barrio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HabilitacionBarrioService {

    private final PerfilRepository perfilRepository;
    private final UsuarioActual usuarioActual;

    /**
     * US-33: solo perfiles Aprobados, en orden alfabético por razón social o nombre y paginados (RF-15, RNF-08).
     * Pendientes, rechazados y suspendidos no aparecen.
     */
    @Transactional(readOnly = true)
    public PageResponse<PerfilBarrioResponse> listar(Pageable pageable) {
        Long barrioId = usuarioActual.barrioId();
        Page<Perfil> pagina = perfilRepository.listarConFiltros(EstadoPerfil.APROBADO, null, pageable);

        List<Long> ids = pagina.getContent().stream().map(Perfil::getId).toList();
        Set<Long> habilitados = ids.isEmpty()
            ? Set.of()
            : new HashSet<>(perfilRepository.perfilesHabilitadosEnBarrio(barrioId, ids));

        return PageResponse.from(pagina.map(p -> PerfilBarrioResponse.from(p, habilitados.contains(p.getId()))));
    }

    /** US-34 CA-1: habilita el perfil en el barrio del Operador. No cambia su estado global ni otros barrios. */
    @Transactional
    public PerfilBarrioResponse habilitar(Long perfilId) {
        Long barrioId = usuarioActual.barrioId();
        Perfil perfil = buscarAprobado(perfilId);
        perfilRepository.habilitarEnBarrio(perfil.getId(), barrioId);
        log.info("Perfil {} habilitado en el barrio {}", perfil.getId(), barrioId);
        return PerfilBarrioResponse.from(perfil, true);
    }

    /**
     * US-34 CA-2: deshabilita el perfil en el barrio del Operador. Los ingresos abiertos no se tocan: el Guardia
     * puede registrar el egreso igual, porque el egreso no tiene verificaciones (RF-20, US-34 CA-4).
     */
    @Transactional
    public PerfilBarrioResponse deshabilitar(Long perfilId) {
        Long barrioId = usuarioActual.barrioId();
        Perfil perfil = buscarAprobado(perfilId);
        perfilRepository.deshabilitarEnBarrio(perfil.getId(), barrioId);
        log.info("Perfil {} deshabilitado en el barrio {}", perfil.getId(), barrioId);
        return PerfilBarrioResponse.from(perfil, false);
    }

    /** Un perfil que no está Aprobado no figura en el listado del Operador: responde como inexistente. */
    private Perfil buscarAprobado(Long perfilId) {
        return perfilRepository.findById(perfilId)
            .filter(p -> p.getEstado() == EstadoPerfil.APROBADO)
            .orElseThrow(NotFoundException::new);
    }
}
