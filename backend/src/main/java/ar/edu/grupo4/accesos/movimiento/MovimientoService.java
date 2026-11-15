package ar.edu.grupo4.accesos.movimiento;

import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.movimiento.dto.BarrioFiltroResponse;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoPropioFiltros;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoPropioResponse;
import ar.edu.grupo4.accesos.usuario.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de movimientos e historiales de accesos (US-31 y US-46).
 */
@Service
@RequiredArgsConstructor
public class MovimientoService {

    private final MovimientoRepository repository;
    private final UsuarioActual usuarioActual;

    /**
     * US-12 CA-5: al desactivar un barrio se registra el egreso automático de los ingresos que estaban abiertos.
     */
    @Transactional
    public int registrarEgresosAutomaticos(Long barrioId, String motivo) {
        return repository.registrarEgresosAutomaticos(barrioId, motivo);
    }

    /**
     * Consulta el historial propio de la Empresa o Monotributista autenticado (aislamiento RI-02, RN-14).
     */
    @Transactional(readOnly = true)
    public PageResponse<MovimientoPropioResponse> consultarHistorialPropio(MovimientoPropioFiltros filtros, Pageable pageable) {
        Long perfilId = usuarioActual.perfilIdAprobado();
        return repository.consultarHistorialPropio(perfilId, filtros, pageable);
    }

    /**
     * Lista los barrios donde el perfil autenticado está habilitado.
     */
    @Transactional(readOnly = true)
    public List<BarrioFiltroResponse> listarBarriosHabilitados() {
        Long perfilId = usuarioActual.perfilIdAprobado();
        return repository.listarBarriosHabilitados(perfilId);
    }
}
