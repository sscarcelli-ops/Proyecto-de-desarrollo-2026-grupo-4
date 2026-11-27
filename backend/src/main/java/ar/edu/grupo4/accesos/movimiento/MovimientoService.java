package ar.edu.grupo4.accesos.movimiento;

import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.movimiento.dto.EntidadFiltroResponse;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoFiltros;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoHistorialResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovimientoService {

    private final MovimientoRepository movimientoRepository;

    /**
     * Cierra con un egreso automático los ingresos abiertos de un barrio (US-12: al desactivarlo). Se exige una
     * transacción en curso (MANDATORY): los egresos se registran junto con el cambio de estado, o ninguno de los dos.
     *
     * @return cuántos egresos se registraron
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int registrarEgresosAutomaticos(Long barrioId, String motivo) {
        return movimientoRepository.registrarEgresosAutomaticos(barrioId, motivo);
    }

    /**
     * US-36: Consulta paginada del historial de accesos del barrio.
     */
    @Transactional(readOnly = true)
    public PageResponse<MovimientoHistorialResponse> consultarHistorial(Long barrioId, MovimientoFiltros filtros, Pageable pageable) {
        return movimientoRepository.consultarHistorial(barrioId, filtros, pageable);
    }

    /**
     * US-36: Empresas y Monotributistas con al menos un movimiento en el barrio, para el filtro. Incluye a los que
     * hoy están deshabilitados o suspendidos: su historial se sigue pudiendo consultar.
     */
    @Transactional(readOnly = true)
    public List<EntidadFiltroResponse> listarEntidadesFiltro(Long barrioId) {
        return movimientoRepository.listarEntidadesDelBarrio(barrioId);
    }
}
