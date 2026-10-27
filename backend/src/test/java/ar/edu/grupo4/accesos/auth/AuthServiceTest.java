package ar.edu.grupo4.accesos.auth;

import ar.edu.grupo4.accesos.auth.dto.LoginRequest;
import ar.edu.grupo4.accesos.auth.dto.LoginResponse;
import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.security.TokenService;
import ar.edu.grupo4.accesos.common.security.TokenService.TokenEmitido;
import ar.edu.grupo4.accesos.usuario.EstadoAcceso;
import ar.edu.grupo4.accesos.usuario.Usuario;
import ar.edu.grupo4.accesos.usuario.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    // Strength 4: mismo algoritmo que en producción, pero rápido para los tests
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final TokenService tokenService = mock(TokenService.class);
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(usuarioService, encoder, tokenService);
        Usuario admin = new Usuario();
        admin.setId(1L);
        admin.setEmail("admin@seed.test");
        admin.setPasswordHash(encoder.encode("Prueba123"));
        when(usuarioService.buscarPorEmail(anyString())).thenReturn(Optional.empty());
        when(usuarioService.buscarPorEmail("admin@seed.test")).thenReturn(Optional.of(admin));
        when(usuarioService.estadoDeAcceso(anyLong())).thenReturn(EstadoAcceso.HABILITADO);
        when(tokenService.emitir(any())).thenReturn(new TokenEmitido("token-de-prueba", Instant.EPOCH));
    }

    @Test
    void credencialesCorrectasDevuelveElTokenYElUsuario() {
        when(usuarioService.nombreVisible(1L)).thenReturn(Optional.of("Carlos Gómez"));

        LoginResponse response = authService.login(new LoginRequest("admin@seed.test", "Prueba123"));

        assertThat(response.token()).isEqualTo("token-de-prueba");
        assertThat(response.email()).isEqualTo("admin@seed.test");
        assertThat(response.nombre()).isEqualTo("Carlos Gómez");
    }

    @Test
    void sinNombreVisibleElNombreVieneNulo() {
        when(usuarioService.nombreVisible(1L)).thenReturn(Optional.empty());

        assertThat(authService.login(new LoginRequest("admin@seed.test", "Prueba123")).nombre()).isNull();
    }

    @Test
    void contrasenaIncorrectaDa401ConElMensajeDeLaUsYNoEmiteToken() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@seed.test", "Otra123")))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Usuario o contraseña incorrectos")
            .extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(tokenService, never()).emitir(any());
    }

    @Test
    void emailInexistenteDaElMismoErrorQueContrasenaIncorrecta() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("nadie@seed.test", "Prueba123")))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Usuario o contraseña incorrectos")
            .extracting("code").isEqualTo("CREDENCIALES_INVALIDAS");
    }

    // CA-3: con la contraseña correcta, no entra y ve el mensaje correspondiente
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "SUSPENDIDO          | CUENTA_SUSPENDIDA     | Tu cuenta se encuentra suspendida",
        "DESHABILITADO       | USUARIO_DESHABILITADO | Tu usuario se encuentra deshabilitado",
        "BARRIO_INACTIVO     | BARRIO_INACTIVO       | Tu barrio se encuentra inactivo",
        "OPERADOR_SIN_BARRIO | OPERADOR_SIN_BARRIO   | No tenés un barrio asignado. Contactá al Administrador.",
    })
    void cuentaQueNoPuedeOperarDa403ConSuMensajeYNoEmiteToken(EstadoAcceso estado, String codigo, String mensaje) {
        when(usuarioService.estadoDeAcceso(1L)).thenReturn(estado);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@seed.test", "Prueba123")))
            .isInstanceOf(BusinessException.class)
            .hasMessage(mensaje)
            .hasFieldOrPropertyWithValue("code", codigo)
            .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
        verify(tokenService, never()).emitir(any());
    }

    @Test
    void conContrasenaIncorrectaNoSeConsultaNiRevelaElEstadoDeLaCuenta() {
        when(usuarioService.estadoDeAcceso(1L)).thenReturn(EstadoAcceso.SUSPENDIDO);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@seed.test", "Otra123")))
            .hasMessage("Usuario o contraseña incorrectos");
        verify(usuarioService, never()).estadoDeAcceso(anyLong());
    }
}
