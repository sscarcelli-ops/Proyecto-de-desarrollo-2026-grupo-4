package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.OperadorRequest;
import ar.edu.grupo4.accesos.barrio.dto.OperadorResponse;
import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.usuario.Usuario;
import ar.edu.grupo4.accesos.usuario.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ar.edu.grupo4.accesos.barrio.dto.OperadorListadoResponse;
import ar.edu.grupo4.accesos.common.paging.PageResponse;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperadorServiceTest {

    private final OperadorRepository operadorRepository = mock(OperadorRepository.class);
    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private OperadorService service;

    @BeforeEach
    void setUp() {
        service = new OperadorService(operadorRepository, usuarioService);
        when(operadorRepository.save(any(Operador.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static OperadorRequest request() {
        return new OperadorRequest(" Olga ", "Operadora ", "olga@barrio.test", " 1145550001 ", "Prueba123");
    }

    private static Usuario cuentaCreada() {
        Usuario usuario = new Usuario();
        usuario.setId(9L);
        usuario.setEmail("olga@barrio.test");
        return usuario;
    }

    @Test
    void crearHaceLaCuentaConRolOperadorYGuardaSusDatos() {
        when(usuarioService.existePorEmail("olga@barrio.test")).thenReturn(false);
        when(usuarioService.crearConRol("olga@barrio.test", "Prueba123", "OPERADOR")).thenReturn(cuentaCreada());

        OperadorResponse respuesta = service.crear(request());

        ArgumentCaptor<Operador> guardado = ArgumentCaptor.forClass(Operador.class);
        verify(operadorRepository).save(guardado.capture());
        assertThat(guardado.getValue().getUsuarioId()).isEqualTo(9L);
        assertThat(respuesta.id()).isEqualTo(9L);
        assertThat(respuesta.email()).isEqualTo("olga@barrio.test");
    }

    @Test
    void crearGuardaLosDatosSinEspaciosSobrantes() {
        when(usuarioService.existePorEmail(anyString())).thenReturn(false);
        when(usuarioService.crearConRol(anyString(), anyString(), anyString())).thenReturn(cuentaCreada());

        OperadorResponse respuesta = service.crear(request());

        assertThat(respuesta.nombre()).isEqualTo("Olga");
        assertThat(respuesta.apellido()).isEqualTo("Operadora");
        assertThat(respuesta.telefono()).isEqualTo("1145550001");
    }

    @Test
    void crearConUnEmailYaRegistradoFallaYNoCreaNada() {
        when(usuarioService.existePorEmail("olga@barrio.test")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(request()))
            .isInstanceOfSatisfying(BusinessException.class, e -> {
                assertThat(e.getCode()).isEqualTo("EMAIL_DUPLICADO");
                assertThat(e.getMessage()).isEqualTo("Este email ya se encuentra registrado");
            });
        verify(usuarioService, never()).crearConRol(anyString(), anyString(), anyString());
        verify(operadorRepository, never()).save(any(Operador.class));
    }

    @Test
    void buscarPorIdsDevuelveLosOperadoresIndexadosPorId() {
        when(operadorRepository.findAllById(List.of(3L, 4L))).thenReturn(List.of(
            new Operador(3L, "Olga", "Operadora", "1145550001"),
            new Operador(4L, "Oscar", "Libre", "1145550002")));

        assertThat(service.buscarPorIds(List.of(3L, 4L))).containsOnlyKeys(3L, 4L);
    }

    private static OperadorRepository.OperadorListado fila(long id, String nombre, String apellido, String email, Long barrioId, String barrioNombre) {
        OperadorRepository.OperadorListado fila = mock(OperadorRepository.OperadorListado.class);
        when(fila.getId()).thenReturn(id);
        when(fila.getNombre()).thenReturn(nombre);
        when(fila.getApellido()).thenReturn(apellido);
        when(fila.getEmail()).thenReturn(email);
        when(fila.getTelefono()).thenReturn("1145550001");
        when(fila.getBarrioId()).thenReturn(barrioId);
        when(fila.getBarrioNombre()).thenReturn(barrioNombre);
        return fila;
    }

    @Test
    void listarSinBarrioFiguraDisponibleYConBarrioOcupado() {
        Pageable pageable = PageRequest.of(0, 10);
        // Las filas se arman antes de stubbear: crear un mock (fila) dentro de thenReturn deja el stubbing a medias
        List<OperadorRepository.OperadorListado> filas = List.of(
            fila(1L, "Olga", "Operadora", "olga@barrio.test", 7L, "Barrio Los Álamos"),
            fila(2L, "Oscar", "Libre", "oscar@barrio.test", null, null));
        when(operadorRepository.listarConFiltros(isNull(), isNull(), eq(pageable)))
            .thenReturn(new PageImpl<>(filas, pageable, 2));

        PageResponse<OperadorListadoResponse> pagina = service.listar(null, null, pageable);

        assertThat(pagina.totalElements()).isEqualTo(2);
        assertThat(pagina.content()).extracting(OperadorListadoResponse::estado).containsExactly("OCUPADO", "DISPONIBLE");
        assertThat(pagina.content().get(0).barrioNombre()).isEqualTo("Barrio Los Álamos");
        assertThat(pagina.content().get(1).barrioId()).isNull();
        assertThat(pagina.content().get(1).email()).isEqualTo("oscar@barrio.test");
    }

    @Test
    void listarPasaElEstadoYEscapaLosComodinesDelTexto() {
        Pageable pageable = PageRequest.of(0, 10);
        when(operadorRepository.listarConFiltros(any(), any(), any())).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.listar(EstadoOperador.OCUPADO, " 50%_a\\b ", pageable);

        // % y _ llegan como texto (con barra), y la propia barra también; el texto va sin espacios al inicio ni al final
        verify(operadorRepository).listarConFiltros("OCUPADO", "50\\%\\_a\\\\b", pageable);
    }

    @Test
    void listarConTextoEnBlancoNoFiltraPorTexto() {
        Pageable pageable = PageRequest.of(0, 10);
        when(operadorRepository.listarConFiltros(any(), any(), any())).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.listar(EstadoOperador.DISPONIBLE, "   ", pageable);

        verify(operadorRepository).listarConFiltros("DISPONIBLE", null, pageable);
    }

    @Test
    void cerrarSesionesDelOperadorCierraLasSesionesDeSuCuenta() {
        service.cerrarSesiones(7L);

        verify(usuarioService).cerrarSesiones(7L);
    }
}
