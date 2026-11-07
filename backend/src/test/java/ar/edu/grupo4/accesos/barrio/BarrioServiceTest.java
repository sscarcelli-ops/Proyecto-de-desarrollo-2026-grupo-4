package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.AsignarOperadorRequest;
import ar.edu.grupo4.accesos.barrio.dto.BarrioRequest;
import ar.edu.grupo4.accesos.barrio.dto.OperadorRequest;
import ar.edu.grupo4.accesos.barrio.dto.OperadorResponse;
import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
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

    private final BarrioRepository repository = mock(BarrioRepository.class);
    private final OperadorService operadorService = mock(OperadorService.class);
    private BarrioService service;

    @BeforeEach
    void setUp() {
        service = new BarrioService(repository, operadorService);
        when(repository.save(any(Barrio.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.saveAndFlush(any(Barrio.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static Barrio barrioExistente() {
        Barrio barrio = new Barrio("Los Álamos", "Pilar, Buenos Aires", "Ruta 8 km 50");
        barrio.setId(7L);
        barrio.setEstado(EstadoBarrio.ACTIVO);
        barrio.setOperadorId(3L);
        return barrio;
    }

    private static Barrio barrioSinOperador() {
        Barrio barrio = new Barrio("San Jorge", "Escobar, Buenos Aires", "Calle 1");
        barrio.setId(8L);
        return barrio;
    }

    // ---- US-10 y US-11

    @Test
    void crearDejaElBarrioInactivoYSinOperador() {
        Barrio creado = service.crear(new BarrioRequest("Barrio Test", "Baradero, Buenos Aires", "Calle 1"));

        assertThat(creado.getEstado()).isEqualTo(EstadoBarrio.INACTIVO);
        assertThat(creado.getOperadorId()).isNull();
        verify(repository).saveAndFlush(creado);
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
        verify(repository).saveAndFlush(modificado);
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
        when(repository.existsByNombreIgnoreCaseAndLocalidadProvinciaIgnoreCase("Barrio Test", "Baradero"))
            .thenReturn(true);

        assertThatThrownBy(() -> service.crear(new BarrioRequest(" Barrio Test ", "Baradero ", "Calle 1")))
            .isInstanceOfSatisfying(BusinessException.class, e -> {
                assertThat(e.getCode()).isEqualTo("BARRIO_DUPLICADO");
                assertThat(e.getMessage()).isEqualTo("Ya existe un barrio con ese nombre en esa localidad/provincia");
            });
        verify(repository, never()).saveAndFlush(any(Barrio.class));
    }

    @Test
    void siLaBaseRechazaElBarrioPorqueOtroPedidoIgualLlegoPrimeroSeInformaQueEstaDuplicado() {
        when(repository.saveAndFlush(any(Barrio.class))).thenThrow(new DataIntegrityViolationException("uq"));

        assertThatThrownBy(() -> service.crear(new BarrioRequest("Barrio Test", "Baradero", "Calle 1")))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("BARRIO_DUPLICADO"));
    }

    @Test
    void modificarUnBarrioConElNombreYLaLocalidadDeOtroFalla() {
        when(repository.findById(7L)).thenReturn(Optional.of(barrioExistente()));
        when(repository.existsByNombreIgnoreCaseAndLocalidadProvinciaIgnoreCaseAndIdNot("San Jorge", "Escobar", 7L))
            .thenReturn(true);

        assertThatThrownBy(() -> service.modificar(7L, new BarrioRequest("San Jorge", "Escobar", "Calle 1")))
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
