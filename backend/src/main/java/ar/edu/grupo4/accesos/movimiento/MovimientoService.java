package ar.edu.grupo4.accesos.movimiento;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
}
