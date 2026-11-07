package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.OperadorRequest;
import ar.edu.grupo4.accesos.barrio.dto.OperadorResponse;
import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.usuario.Usuario;
import ar.edu.grupo4.accesos.usuario.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
}
