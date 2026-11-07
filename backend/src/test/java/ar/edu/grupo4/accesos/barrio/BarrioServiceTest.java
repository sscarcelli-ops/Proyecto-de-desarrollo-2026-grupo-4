package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.BarrioRequest;
import ar.edu.grupo4.accesos.barrio.dto.BarrioResponse;
import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.ubicacion.Localidad;
import ar.edu.grupo4.accesos.ubicacion.Provincia;
import ar.edu.grupo4.accesos.ubicacion.UbicacionService;
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

    private static final Provincia BUENOS_AIRES = new Provincia("06", "Buenos Aires");
    private static final Localidad BARADERO = new Localidad("06070010", "Baradero", "Baradero", BUENOS_AIRES);
    private static final Localidad PILAR = new Localidad("06638040", "Pilar", "Pilar", BUENOS_AIRES);

    private final BarrioRepository repository = mock(BarrioRepository.class);
    private final UbicacionService ubicacionService = mock(UbicacionService.class);
    private BarrioService service;

    @BeforeEach
    void setUp() {
        service = new BarrioService(repository, ubicacionService);
        when(repository.save(any(Barrio.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ubicacionService.obtenerLocalidad(BARADERO.getId())).thenReturn(BARADERO);
        when(ubicacionService.obtenerLocalidad("99999999"))
            .thenThrow(new BusinessException("LOCALIDAD_INEXISTENTE", "La localidad no existe"));
    }

    private static Barrio barrioExistente() {
        Barrio barrio = new Barrio("Los Álamos", PILAR, "Ruta 8 km 50");
        barrio.setId(7L);
        barrio.setEstado(EstadoBarrio.ACTIVO);
        barrio.setOperadorId(3L);
        return barrio;
    }

    @Test
    void crearDejaElBarrioInactivoYSinOperador() {
        Barrio creado = service.crear(new BarrioRequest("Barrio Test", BARADERO.getId(), "Calle 1"));

        assertThat(creado.getEstado()).isEqualTo(EstadoBarrio.INACTIVO);
        assertThat(creado.getOperadorId()).isNull();
        verify(repository).save(creado);
    }

    @Test
    void crearGuardaLosDatosSinEspaciosSobrantesYLaLocalidadDelCatalogo() {
        Barrio creado = service.crear(new BarrioRequest("  Barrio Test ", BARADERO.getId(), "  Calle 1  "));

        assertThat(creado.getNombre()).isEqualTo("Barrio Test");
        assertThat(creado.getLocalidad()).isSameAs(BARADERO);
        assertThat(creado.getDireccion()).isEqualTo("Calle 1");
    }

    @Test
    void crearConUnaLocalidadQueNoEstaEnElCatalogoNoGuarda() {
        assertThatThrownBy(() -> service.crear(new BarrioRequest("Barrio Test", "99999999", "Calle 1")))
            .isInstanceOf(BusinessException.class)
            .hasMessage("La localidad no existe");
        verify(repository, never()).save(any(Barrio.class));
    }

    @Test
    void laRespuestaSeparaProvinciaYLocalidad() {
        BarrioResponse r = BarrioResponse.from(service.crear(new BarrioRequest("Barrio Test", BARADERO.getId(), "Calle 1")));

        assertThat(r.provinciaId()).isEqualTo("06");
        assertThat(r.provincia()).isEqualTo("Buenos Aires");
        assertThat(r.localidadId()).isEqualTo("06070010");
        assertThat(r.localidad()).isEqualTo("Baradero");
    }

    @Test
    void unBarrioAnteriorSinLocalidadSeRespondeSinUbicacion() {
        Barrio viejo = barrioExistente();
        viejo.setLocalidad(null);

        BarrioResponse r = BarrioResponse.from(viejo);

        assertThat(r.provincia()).isNull();
        assertThat(r.localidad()).isNull();
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

        Barrio modificado = service.modificar(7L, new BarrioRequest(" Álamos II ", BARADERO.getId(), "Calle Nueva 123 "));

        assertThat(modificado.getNombre()).isEqualTo("Álamos II");
        assertThat(modificado.getLocalidad()).isSameAs(BARADERO);
        assertThat(modificado.getDireccion()).isEqualTo("Calle Nueva 123");
        verify(repository).save(modificado);
    }

    @Test
    void modificarNoCambiaElEstadoNiElOperador() {
        when(repository.findById(7L)).thenReturn(Optional.of(barrioExistente()));

        Barrio modificado = service.modificar(7L, new BarrioRequest("Álamos II", BARADERO.getId(), "Calle Nueva 123"));

        assertThat(modificado.getEstado()).isEqualTo(EstadoBarrio.ACTIVO);
        assertThat(modificado.getOperadorId()).isEqualTo(3L);
    }

    @Test
    void modificarConUnaLocalidadQueNoEstaEnElCatalogoNoGuarda() {
        when(repository.findById(7L)).thenReturn(Optional.of(barrioExistente()));

        assertThatThrownBy(() -> service.modificar(7L, new BarrioRequest("Álamos II", "99999999", "Calle 1")))
            .isInstanceOf(BusinessException.class);
        verify(repository, never()).save(any(Barrio.class));
    }

    @Test
    void modificarUnBarrioInexistenteDevuelveNotFoundYNoGuarda() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modificar(99L, new BarrioRequest("A", BARADERO.getId(), "C")))
            .isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any(Barrio.class));
    }
}
