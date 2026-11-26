package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.OperadorModificarRequest;
import ar.edu.grupo4.accesos.barrio.dto.OperadorResponse;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.usuario.Usuario;
import ar.edu.grupo4.accesos.usuario.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** US-16: modificar nombre, apellido y teléfono de un Operador; el email solo se muestra. */
class OperadorModificarServiceTest {

    private final OperadorRepository operadorRepository = mock(OperadorRepository.class);
    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private OperadorService service;

    @BeforeEach
    void setUp() {
        service = new OperadorService(operadorRepository, usuarioService);
        when(operadorRepository.save(any(Operador.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static Operador olga() {
        return new Operador(7L, "Olga", "Operadora", "1145550001");
    }

    private static Usuario cuenta() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setEmail("operador@seed.test");
        return usuario;
    }

    @Test
    @DisplayName("US-16 CA-1: modificar guarda nombre, apellido y teléfono sin espacios sobrantes")
    void modificarGuardaLosDatosNuevos() {
        when(operadorRepository.findById(7L)).thenReturn(Optional.of(olga()));
        when(usuarioService.buscarPorId(7L)).thenReturn(Optional.of(cuenta()));

        OperadorResponse respuesta = service.modificar(7L,
            new OperadorModificarRequest(" Olivia ", " Operaria ", " 1145559999 "));

        assertThat(respuesta.id()).isEqualTo(7L);
        assertThat(respuesta.nombre()).isEqualTo("Olivia");
        assertThat(respuesta.apellido()).isEqualTo("Operaria");
        assertThat(respuesta.telefono()).isEqualTo("1145559999");
        verify(operadorRepository).save(any(Operador.class));
    }

    @Test
    @DisplayName("US-16 CA-2: el email no cambia al modificar (la respuesta trae el de la cuenta) y no se toca la cuenta")
    void modificarNoTocaElEmailNiLaCuenta() {
        when(operadorRepository.findById(7L)).thenReturn(Optional.of(olga()));
        when(usuarioService.buscarPorId(7L)).thenReturn(Optional.of(cuenta()));

        OperadorResponse respuesta = service.modificar(7L, new OperadorModificarRequest("Olga", "Operadora", "1145550001"));

        assertThat(respuesta.email()).isEqualTo("operador@seed.test");
        verify(usuarioService, never()).cerrarSesiones(any());
    }

    @Test
    @DisplayName("US-16: modificar un Operador que no existe responde 404 y no guarda nada")
    void modificarUnOperadorInexistenteDa404() {
        when(operadorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modificar(99L, new OperadorModificarRequest("A", "B", "1")))
            .isInstanceOf(NotFoundException.class);
        verify(operadorRepository, never()).save(any(Operador.class));
        verifyNoInteractions(usuarioService);
    }

    @Test
    @DisplayName("US-16 CA-2: obtener trae los datos del Operador con su email")
    void obtenerTraeElEmailDeLaCuenta() {
        when(operadorRepository.findById(7L)).thenReturn(Optional.of(olga()));
        when(usuarioService.buscarPorId(7L)).thenReturn(Optional.of(cuenta()));

        OperadorResponse respuesta = service.obtener(7L);

        assertThat(respuesta.nombre()).isEqualTo("Olga");
        assertThat(respuesta.email()).isEqualTo("operador@seed.test");
        assertThat(respuesta.telefono()).isEqualTo("1145550001");
    }

    @Test
    @DisplayName("US-16: obtener un Operador que no existe responde 404")
    void obtenerUnOperadorInexistenteDa404() {
        when(operadorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(NotFoundException.class);
    }
}
