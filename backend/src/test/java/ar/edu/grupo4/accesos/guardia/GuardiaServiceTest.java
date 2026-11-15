package ar.edu.grupo4.accesos.guardia;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.guardia.dto.GuardiaModificarRequest;
import ar.edu.grupo4.accesos.guardia.dto.GuardiaRequest;
import ar.edu.grupo4.accesos.guardia.dto.GuardiaResponse;
import ar.edu.grupo4.accesos.usuario.Usuario;
import ar.edu.grupo4.accesos.usuario.UsuarioActual;
import ar.edu.grupo4.accesos.usuario.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuardiaServiceTest {

    private static final long BARRIO = 5L;

    private final GuardiaRepository guardiaRepository = mock(GuardiaRepository.class);
    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final UsuarioActual usuarioActual = mock(UsuarioActual.class);
    private GuardiaService service;

    @BeforeEach
    void setUp() {
        service = new GuardiaService(guardiaRepository, usuarioService, usuarioActual);
        when(usuarioActual.barrioId()).thenReturn(BARRIO);
        when(guardiaRepository.save(any(Guardia.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static Usuario cuenta(long id, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        return usuario;
    }

    private static Guardia guardia(long id) {
        return new Guardia(id, BARRIO, "Gabriel", "Guardia");
    }

    @Test
    void crearHaceLaCuentaConRolGuardiaYLoAsociaAlBarrioDelOperador() {
        when(usuarioService.existePorEmail("gabi@barrio.test")).thenReturn(false);
        when(usuarioService.crearConRol("gabi@barrio.test", "Prueba123", "GUARDIA"))
            .thenReturn(cuenta(9L, "gabi@barrio.test"));

        GuardiaResponse respuesta = service.crear(new GuardiaRequest(" Gabriel ", "Guardia ", "gabi@barrio.test", "Prueba123"));

        ArgumentCaptor<Guardia> guardado = ArgumentCaptor.forClass(Guardia.class);
        verify(guardiaRepository).save(guardado.capture());
        assertThat(guardado.getValue().getUsuarioId()).isEqualTo(9L);
        assertThat(guardado.getValue().getBarrioId()).isEqualTo(BARRIO);
        assertThat(guardado.getValue().getNombre()).isEqualTo("Gabriel");
        assertThat(guardado.getValue().isHabilitado()).isTrue();
        assertThat(respuesta.email()).isEqualTo("gabi@barrio.test");
    }

    @Test
    void crearConEmailYaRegistradoNoGuarda() {
        when(usuarioService.existePorEmail("gabi@barrio.test")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new GuardiaRequest("Gabriel", "Guardia", "gabi@barrio.test", "Prueba123")))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Este email ya se encuentra registrado");
        verify(guardiaRepository, never()).save(any());
    }

    @Test
    void listarMuestraSoloLosDelBarrioConSuEmail() {
        when(guardiaRepository.findByBarrioIdOrderByApellidoAscNombreAsc(BARRIO)).thenReturn(List.of(guardia(9L)));
        when(usuarioService.buscarPorIds(List.of(9L))).thenReturn(Map.of(9L, cuenta(9L, "gabi@barrio.test")));

        List<GuardiaResponse> lista = service.listar();

        assertThat(lista).singleElement().satisfies(g -> {
            assertThat(g.id()).isEqualTo(9L);
            assertThat(g.email()).isEqualTo("gabi@barrio.test");
            assertThat(g.habilitado()).isTrue();
        });
    }

    @Test
    void unGuardiaDeOtroBarrioSeRechazaComoSiNoExistiera() {
        when(guardiaRepository.findByUsuarioIdAndBarrioId(77L, BARRIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(77L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.modificar(77L, new GuardiaModificarRequest("A", "B", "a@b.test", null)))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.cambiarEstado(77L, false)).isInstanceOf(NotFoundException.class);
        verify(usuarioService, never()).actualizarCuenta(any(), anyString(), any());
    }

    @Test
    void modificarSinContrasenaNoLaCambia() {
        Guardia existente = guardia(9L);
        when(guardiaRepository.findByUsuarioIdAndBarrioId(9L, BARRIO)).thenReturn(Optional.of(existente));
        when(usuarioService.actualizarCuenta(9L, "nuevo@barrio.test", null)).thenReturn(cuenta(9L, "nuevo@barrio.test"));

        GuardiaResponse respuesta = service.modificar(9L, new GuardiaModificarRequest(" Gabi ", " G ", "nuevo@barrio.test", "  "));

        verify(usuarioService).actualizarCuenta(9L, "nuevo@barrio.test", null);
        assertThat(existente.getNombre()).isEqualTo("Gabi");
        assertThat(respuesta.email()).isEqualTo("nuevo@barrio.test");
    }

    @Test
    void modificarConContrasenaNuevaValidaLaEnviaAActualizar() {
        when(guardiaRepository.findByUsuarioIdAndBarrioId(9L, BARRIO)).thenReturn(Optional.of(guardia(9L)));
        when(usuarioService.actualizarCuenta(9L, "gabi@barrio.test", "Nueva1234")).thenReturn(cuenta(9L, "gabi@barrio.test"));

        service.modificar(9L, new GuardiaModificarRequest("Gabriel", "Guardia", "gabi@barrio.test", "Nueva1234"));

        verify(usuarioService).actualizarCuenta(9L, "gabi@barrio.test", "Nueva1234");
    }

    @Test
    void modificarConContrasenaDebilNoCambiaNada() {
        Guardia existente = guardia(9L);
        when(guardiaRepository.findByUsuarioIdAndBarrioId(9L, BARRIO)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.modificar(9L, new GuardiaModificarRequest("Otro", "Nombre", "gabi@barrio.test", "debil")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("al menos 8 caracteres");
        verify(usuarioService, never()).actualizarCuenta(any(), anyString(), any());
        assertThat(existente.getNombre()).isEqualTo("Gabriel");
    }

    @Test
    void deshabilitarYHabilitarCambianSoloElEstado() {
        Guardia existente = guardia(9L);
        when(guardiaRepository.findByUsuarioIdAndBarrioId(9L, BARRIO)).thenReturn(Optional.of(existente));
        when(usuarioService.buscarPorId(9L)).thenReturn(Optional.of(cuenta(9L, "gabi@barrio.test")));

        assertThat(service.cambiarEstado(9L, false).habilitado()).isFalse();
        assertThat(existente.isHabilitado()).isFalse();
        assertThat(service.cambiarEstado(9L, true).habilitado()).isTrue();
        verify(usuarioService, never()).actualizarCuenta(any(), anyString(), isNull());
    }
}
