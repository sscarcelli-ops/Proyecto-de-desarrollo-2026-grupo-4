package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.AsignarOperadorRequest;
import ar.edu.grupo4.accesos.barrio.dto.BarrioRequest;
import ar.edu.grupo4.accesos.barrio.dto.BarrioResponse;
import ar.edu.grupo4.accesos.barrio.dto.OperadorRequest;
import ar.edu.grupo4.accesos.barrio.dto.OperadorResponse;
import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.ubicacion.Localidad;
import ar.edu.grupo4.accesos.ubicacion.Provincia;
import ar.edu.grupo4.accesos.ubicacion.UbicacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
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
    private static final Localidad ESCOBAR = new Localidad("06252010", "Escobar", "Escobar", BUENOS_AIRES);

    private final BarrioRepository repository = mock(BarrioRepository.class);
    private final OperadorService operadorService = mock(OperadorService.class);
    private final UbicacionService ubicacionService = mock(UbicacionService.class);
    private BarrioService service;

    @BeforeEach
    void setUp() {
        service = new BarrioService(repository, operadorService, ubicacionService);
        when(repository.saveAndFlush(any(Barrio.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ubicacionService.obtenerLocalidad(BARADERO.getId())).thenReturn(BARADERO);
        when(ubicacionService.obtenerLocalidad(ESCOBAR.getId())).thenReturn(ESCOBAR);
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

    private static Barrio barrioSinOperador() {
        Barrio barrio = new Barrio("San Jorge", ESCOBAR, "Calle 1");
        barrio.setId(8L);
        return barrio;
    }

    // ---- US-10 y US-11 (con la localidad del catálogo, T-11)

    @Test
    void crearDejaElBarrioInactivoYSinOperador() {
        Barrio creado = service.crear(new BarrioRequest("Barrio Test", BARADERO.getId(), "Calle 1"));

        assertThat(creado.getEstado()).isEqualTo(EstadoBarrio.INACTIVO);
        assertThat(creado.getOperadorId()).isNull();
        verify(repository).saveAndFlush(creado);
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
        verify(repository, never()).saveAndFlush(any(Barrio.class));
    }

    @Test
    void laRespuestaSeparaProvinciaYLocalidadYTraeElOperador() {
        Operador operador = new Operador(3L, "Olga", "Operadora", "1145550001");

        BarrioResponse r = BarrioResponse.from(barrioExistente(), operador);

        assertThat(r.provinciaId()).isEqualTo("06");
        assertThat(r.provincia()).isEqualTo("Buenos Aires");
        assertThat(r.localidadId()).isEqualTo(PILAR.getId());
        assertThat(r.localidad()).isEqualTo("Pilar");
        assertThat(r.operadorNombre()).isEqualTo("Olga");
        assertThat(r.operadorApellido()).isEqualTo("Operadora");
    }

    @Test
    void unBarrioAnteriorSinLocalidadNiOperadorSeRespondeConEsosDatosVacios() {
        Barrio viejo = barrioSinOperador();
        viejo.setLocalidad(null);

        BarrioResponse r = BarrioResponse.from(viejo, null);

        assertThat(r.provincia()).isNull();
        assertThat(r.localidad()).isNull();
        assertThat(r.operadorNombre()).isNull();
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
        verify(repository).saveAndFlush(modificado);
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
        verify(repository, never()).saveAndFlush(any(Barrio.class));
    }

    @Test
    void modificarUnBarrioInexistenteDevuelveNotFoundYNoGuarda() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modificar(99L, new BarrioRequest("A", BARADERO.getId(), "C")))
            .isInstanceOf(NotFoundException.class);
        verify(repository, never()).saveAndFlush(any(Barrio.class));
    }

    @Test
    void operadoresDeBuscaSoloLosOperadoresDeLosBarriosQueTienen() {
        Operador operador = new Operador(3L, "Olga", "Operadora", "1145550001");
        when(operadorService.buscarPorIds(List.of(3L))).thenReturn(Map.of(3L, operador));

        Map<Long, Operador> operadores = service.operadoresDe(List.of(barrioExistente(), barrioSinOperador()));

        assertThat(operadores).containsOnlyKeys(3L);
    }

    @Test
    void operadoresDeSinNingunOperadorNoConsultaNada() {
        assertThat(service.operadoresDe(List.of(barrioSinOperador()))).isEmpty();
        verify(operadorService, never()).buscarPorIds(any());
    }

    @Test
    void crearUnBarrioConElMismoNombreEnLaMismaLocalidadFallaYNoGuarda() {
        when(repository.existsByNombreIgnoreCaseAndLocalidadId("Barrio Test", BARADERO.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new BarrioRequest(" Barrio Test ", BARADERO.getId(), "Calle 1")))
            .isInstanceOfSatisfying(BusinessException.class, e -> {
                assertThat(e.getCode()).isEqualTo("BARRIO_DUPLICADO");
                assertThat(e.getMessage()).isEqualTo("Ya existe un barrio con ese nombre en esa localidad");
            });
        verify(repository, never()).saveAndFlush(any(Barrio.class));
    }

    @Test
    void siLaBaseRechazaElBarrioPorqueOtroPedidoIgualLlegoPrimeroSeInformaQueEstaDuplicado() {
        when(repository.saveAndFlush(any(Barrio.class))).thenThrow(new DataIntegrityViolationException("uq"));

        assertThatThrownBy(() -> service.crear(new BarrioRequest("Barrio Test", BARADERO.getId(), "Calle 1")))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("BARRIO_DUPLICADO"));
    }

    @Test
    void modificarUnBarrioConElNombreYLaLocalidadDeOtroFalla() {
        when(repository.findById(7L)).thenReturn(Optional.of(barrioExistente()));
        when(repository.existsByNombreIgnoreCaseAndLocalidadIdAndIdNot("San Jorge", ESCOBAR.getId(), 7L)).thenReturn(true);

        assertThatThrownBy(() -> service.modificar(7L, new BarrioRequest("San Jorge", ESCOBAR.getId(), "Calle 1")))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("BARRIO_DUPLICADO"));
        verify(repository, never()).saveAndFlush(any(Barrio.class));
    }

    // ---- US-45

    private static OperadorRequest operadorNuevo() {
        return new OperadorRequest("Ana", "Pérez", "ana@test.com", "1145559999", "Prueba123");
    }

    @Test
    void asignarUnOperadorDisponibleVinculaYActivaElBarrio() {
        when(repository.findByIdForUpdate(8L)).thenReturn(Optional.of(barrioSinOperador()));
        when(operadorService.existe(5L)).thenReturn(true);
        when(repository.existsByOperadorId(5L)).thenReturn(false);

        Barrio asignado = service.asignarOperador(8L, new AsignarOperadorRequest(5L, null));

        assertThat(asignado.getOperadorId()).isEqualTo(5L);
        assertThat(asignado.getEstado()).isEqualTo(EstadoBarrio.ACTIVO);
    }

    @Test
    void asignarConUnOperadorNuevoLoCreaYLoAsignaEnLaMismaOperacion() {
        when(repository.findByIdForUpdate(8L)).thenReturn(Optional.of(barrioSinOperador()));
        when(operadorService.crear(operadorNuevo()))
            .thenReturn(new OperadorResponse(11L, "Ana", "Pérez", "ana@test.com", "1145559999"));

        Barrio asignado = service.asignarOperador(8L, new AsignarOperadorRequest(null, operadorNuevo()));

        assertThat(asignado.getOperadorId()).isEqualTo(11L);
        assertThat(asignado.getEstado()).isEqualTo(EstadoBarrio.ACTIVO);
        verify(operadorService).crear(operadorNuevo());
    }

    @Test
    void asignarSinIndicarOperadorPideSeleccionarUno() {
        assertThatThrownBy(() -> service.asignarOperador(8L, new AsignarOperadorRequest(null, null)))
            .isInstanceOfSatisfying(BusinessException.class, e -> {
                assertThat(e.getCode()).isEqualTo("OPERADOR_REQUERIDO");
                assertThat(e.getMessage()).isEqualTo("Debe seleccionar un Operador");
            });
        verify(repository, never()).saveAndFlush(any(Barrio.class));
    }

    @Test
    void asignarIndicandoUnOperadorExistenteYUnoNuevoEsAmbiguo() {
        assertThatThrownBy(() -> service.asignarOperador(8L, new AsignarOperadorRequest(5L, operadorNuevo())))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("OPERADOR_AMBIGUO"));
    }

    @Test
    void asignarUnOperadorQueYaTieneOtroBarrioFallaYNoAsigna() {
        Barrio barrio = barrioSinOperador();
        when(repository.findByIdForUpdate(8L)).thenReturn(Optional.of(barrio));
        when(operadorService.existe(5L)).thenReturn(true);
        when(repository.existsByOperadorId(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.asignarOperador(8L, new AsignarOperadorRequest(5L, null)))
            .isInstanceOfSatisfying(BusinessException.class, e -> {
                assertThat(e.getCode()).isEqualTo("OPERADOR_OCUPADO");
                assertThat(e.getMessage()).isEqualTo("El Operador ya está asignado a otro barrio");
            });
        assertThat(barrio.getOperadorId()).isNull();
        assertThat(barrio.getEstado()).isEqualTo(EstadoBarrio.INACTIVO);
        verify(repository, never()).saveAndFlush(any(Barrio.class));
    }

    @Test
    void siLaBaseRechazaElOperadorPorqueOtroLoTomoPrimeroSeInformaQueEstaOcupado() {
        when(repository.findByIdForUpdate(8L)).thenReturn(Optional.of(barrioSinOperador()));
        when(operadorService.existe(5L)).thenReturn(true);
        when(repository.existsByOperadorId(5L)).thenReturn(false);
        when(repository.saveAndFlush(any(Barrio.class))).thenThrow(new DataIntegrityViolationException("uq"));

        assertThatThrownBy(() -> service.asignarOperador(8L, new AsignarOperadorRequest(5L, null)))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("OPERADOR_OCUPADO"));
    }

    @Test
    void asignarAUnBarrioQueYaTieneOperadorFalla() {
        when(repository.findByIdForUpdate(7L)).thenReturn(Optional.of(barrioExistente()));

        assertThatThrownBy(() -> service.asignarOperador(7L, new AsignarOperadorRequest(5L, null)))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("BARRIO_CON_OPERADOR"));
        verify(repository, never()).saveAndFlush(any(Barrio.class));
    }

    @Test
    void asignarAUnBarrioInexistenteDevuelveNotFound() {
        when(repository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.asignarOperador(99L, new AsignarOperadorRequest(5L, null)))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("El barrio no existe.");
    }

    @Test
    void asignarUnOperadorInexistenteDevuelveNotFound() {
        when(repository.findByIdForUpdate(8L)).thenReturn(Optional.of(barrioSinOperador()));
        when(operadorService.existe(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.asignarOperador(8L, new AsignarOperadorRequest(99L, null)))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("El Operador no existe.");
    }
}
