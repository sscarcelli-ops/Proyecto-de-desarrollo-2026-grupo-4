package ar.edu.grupo4.accesos.movimiento;

import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.movimiento.dto.BarrioFiltroResponse;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoPropioFiltros;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoPropioResponse;
import ar.edu.grupo4.accesos.usuario.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoServiceTest {

    @Mock
    private MovimientoRepository repository;

    @Mock
    private UsuarioActual usuarioActual;

    @InjectMocks
    private MovimientoService service;

    private final Long perfilId = 5L;

    @BeforeEach
    void setUp() {
        when(usuarioActual.perfilIdAprobado()).thenReturn(perfilId);
    }

    @Test
    @DisplayName("consultarHistorialPropio delega al repository con el perfilId de la sesión y filtros")
    void consultarHistorialPropio() {
        MovimientoPropioFiltros filtros = new MovimientoPropioFiltros(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 15),
                "30111222",
                1L
        );
        Pageable pageable = PageRequest.of(0, 10);

        MovimientoPropioResponse mockMov = new MovimientoPropioResponse(
                100L,
                OffsetDateTime.now(),
                "INGRESO",
                "Juan",
                "Pérez",
                "30111222",
                "Los Álamos",
                null
        );
        PageResponse<MovimientoPropioResponse> expectedPage = new PageResponse<>(List.of(mockMov), 0, 10, 1, 1);

        when(repository.consultarHistorialPropio(eq(perfilId), eq(filtros), eq(pageable)))
                .thenReturn(expectedPage);

        PageResponse<MovimientoPropioResponse> result = service.consultarHistorialPropio(filtros, pageable);

        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).trabajadorDni()).isEqualTo("30111222");
        assertThat(result.content().get(0).barrioNombre()).isEqualTo("Los Álamos");

        verify(usuarioActual).perfilIdAprobado();
        verify(repository).consultarHistorialPropio(perfilId, filtros, pageable);
    }

    @Test
    @DisplayName("listarBarriosHabilitados delega al repository con el perfilId del usuario autenticado")
    void listarBarriosHabilitados() {
        List<BarrioFiltroResponse> expected = List.of(
                new BarrioFiltroResponse(1L, "Los Álamos"),
                new BarrioFiltroResponse(2L, "Los Robles")
        );

        when(repository.listarBarriosHabilitados(perfilId)).thenReturn(expected);

        List<BarrioFiltroResponse> result = service.listarBarriosHabilitados();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).nombre()).isEqualTo("Los Álamos");
        verify(usuarioActual).perfilIdAprobado();
        verify(repository).listarBarriosHabilitados(perfilId);
    }
}
