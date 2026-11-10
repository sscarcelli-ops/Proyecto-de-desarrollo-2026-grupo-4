package ar.edu.grupo4.accesos.movimiento;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MovimientoServiceTest {

    private final MovimientoRepository repository = mock(MovimientoRepository.class);
    private final MovimientoService service = new MovimientoService(repository);

    @Test
    void registrarEgresosAutomaticosDevuelveCuantosRegistroElRepositorio() {
        when(repository.registrarEgresosAutomaticos(8L, "Motivo")).thenReturn(3);

        assertThat(service.registrarEgresosAutomaticos(8L, "Motivo")).isEqualTo(3);
        verify(repository).registrarEgresosAutomaticos(8L, "Motivo");
    }
}
