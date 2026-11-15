package ar.edu.grupo4.accesos.movimiento;

import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.movimiento.dto.EntidadFiltroResponse;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoFiltros;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoHistorialResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MovimientoServiceTest {

    private final MovimientoRepository repository = mock(MovimientoRepository.class);
    private final MovimientoService service = new MovimientoService(repository);

    @Test
    void registrarEgresosAutomaticosDevuelveCuantosRegistroElRepositorio() {
        when(repository.registrarEgresosAutomaticos(8L, "Motivo")).thenReturn(3);

        assertThat(service.registrarEgresosAutomaticos(8L, "Motivo")).isEqualTo(3);
        verify(repository).registrarEgresosAutomaticos(8L, "Motivo");
    }

    @Test
    @DisplayName("US-36: Delega la consulta paginada del historial en el repositorio")
    void consultarHistorial_delegaEnRepositorio() {
        Long barrioId = 1L;
        MovimientoFiltros filtros = new MovimientoFiltros(LocalDate.now(), LocalDate.now(), "30111222", 10L);
        Pageable pageable = PageRequest.of(0, 10);

        MovimientoHistorialResponse item = new MovimientoHistorialResponse(
                1L, OffsetDateTime.now(), "INGRESO", "Juan", "Pérez", "30111222",
                "EMPRESA", "Limpiezas S.A.", null
        );
        PageResponse<MovimientoHistorialResponse> mockPage = new PageResponse<>(List.of(item), 0, 10, 1, 1);

        when(repository.consultarHistorial(barrioId, filtros, pageable)).thenReturn(mockPage);

        PageResponse<MovimientoHistorialResponse> result = service.consultarHistorial(barrioId, filtros, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).tipo()).isEqualTo("INGRESO");
        verify(repository).consultarHistorial(barrioId, filtros, pageable);
    }

    @Test
    @DisplayName("US-36: Delega el listado de entidades para el filtro en el repositorio")
    void listarEntidadesFiltro_delegaEnRepositorio() {
        Long barrioId = 1L;
        EntidadFiltroResponse entidad = new EntidadFiltroResponse(10L, "Limpiezas S.A.", "EMPRESA");
        when(repository.listarEntidadesDelBarrio(barrioId)).thenReturn(List.of(entidad));

        List<EntidadFiltroResponse> result = service.listarEntidadesFiltro(barrioId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nombre()).isEqualTo("Limpiezas S.A.");
        verify(repository).listarEntidadesDelBarrio(barrioId);
    }
}
