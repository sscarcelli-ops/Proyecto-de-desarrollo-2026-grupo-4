package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.BarrioRequest;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BarrioServiceTest {

    private final BarrioRepository repository = mock(BarrioRepository.class);
    private BarrioService service;

    @BeforeEach
    void setUp() {
        service = new BarrioService(repository);
        when(repository.save(any(Barrio.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static Barrio barrioExistente() {
        Barrio barrio = new Barrio("Los Álamos", "Pilar, Buenos Aires", "Ruta 8 km 50");
        barrio.setId(7L);
        barrio.setEstado(EstadoBarrio.ACTIVO);
        barrio.setOperadorId(3L);
        return barrio;
    }

    @Test
    void crearDejaElBarrioInactivoYSinOperador() {
        Barrio creado = service.crear(new BarrioRequest("Barrio Test", "Baradero, Buenos Aires", "Calle 1"));

        assertThat(creado.getEstado()).isEqualTo(EstadoBarrio.INACTIVO);
        assertThat(creado.getOperadorId()).isNull();
        verify(repository).save(creado);
    }

    @Test
    void crearGuardaLosDatosSinEspaciosSobrantes() {
        Barrio creado = service.crear(new BarrioRequest("  Barrio Test ", " Baradero ", "  Calle 1  "));

        assertThat(creado.getNombre()).isEqualTo("Barrio Test");
        assertThat(creado.getLocalidadProvincia()).isEqualTo("Baradero");
        assertThat(creado.getDireccion()).isEqualTo("Calle 1");
    }

    @Test
    void listarPideLosBarriosOrdenadosAlfabeticamentePorNombre() {
        List<Barrio> barrios = List.of(barrioExistente());
        when(repository.findAll(Sort.by("nombre"))).thenReturn(barrios);

        assertThat(service.listar()).isSameAs(barrios);
    }

    @Test
    void obtenerUnBarrioInexistenteDevuelveNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("El barrio no existe.");
    }

    @Test
    void modificarActualizaNombreLocalidadYDireccion() {
        when(repository.findById(7L)).thenReturn(Optional.of(barrioExistente()));

        Barrio modificado = service.modificar(7L, new BarrioRequest(" Álamos II ", "Escobar, Buenos Aires", "Calle Nueva 123 "));

        assertThat(modificado.getNombre()).isEqualTo("Álamos II");
        assertThat(modificado.getLocalidadProvincia()).isEqualTo("Escobar, Buenos Aires");
        assertThat(modificado.getDireccion()).isEqualTo("Calle Nueva 123");
        verify(repository).save(modificado);
    }

    @Test
    void modificarNoCambiaElEstadoNiElOperador() {
        when(repository.findById(7L)).thenReturn(Optional.of(barrioExistente()));

        Barrio modificado = service.modificar(7L, new BarrioRequest("Álamos II", "Escobar", "Calle Nueva 123"));

        assertThat(modificado.getEstado()).isEqualTo(EstadoBarrio.ACTIVO);
        assertThat(modificado.getOperadorId()).isEqualTo(3L);
    }

    @Test
    void modificarUnBarrioInexistenteDevuelveNotFoundYNoGuarda() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modificar(99L, new BarrioRequest("A", "B", "C")))
            .isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any(Barrio.class));
    }
}
