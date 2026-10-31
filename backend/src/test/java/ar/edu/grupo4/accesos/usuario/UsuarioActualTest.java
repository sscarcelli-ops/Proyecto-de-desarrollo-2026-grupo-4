package ar.edu.grupo4.accesos.usuario;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.security.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** T-03: sesión vigente, estado de la cuenta y aislamiento por barrio y por perfil, sin Spring ni base. */
class UsuarioActualTest {

    private static final long USUARIO_ID = 7L;

    private final UsuarioService usuarioService = mock(UsuarioService.class);
    // Cada test usa una instancia nueva, como cada petición en la app (@RequestScope)
    private final UsuarioActual usuarioActual = new UsuarioActual(usuarioService);

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static void conToken(Object version) {
        Jwt.Builder jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(String.valueOf(USUARIO_ID));
        if (version != null) {
            jwt.claim(TokenService.CLAIM_VERSION, version);
        }
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt.build()));
    }

    private void enLaBase(int version, String estado, Long barrioId, Long perfilAprobadoId) {
        UsuarioRepository.DatosDeSesion datos = new UsuarioRepository.DatosDeSesion() {
            public int getVersionSesion() { return version; }
            public String getEstado() { return estado; }
            public Long getBarrioId() { return barrioId; }
            public Long getPerfilAprobadoId() { return perfilAprobadoId; }
        };
        when(usuarioService.datosDeSesion(USUARIO_ID)).thenReturn(Optional.of(datos));
    }

    private static void rechazaCon(Runnable accion, String codigo, HttpStatus status) {
        assertThatThrownBy(accion::run)
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", codigo)
            .hasFieldOrPropertyWithValue("status", status);
    }

    @Test
    void sesionConLaMismaVersionYCuentaHabilitadaPasa() {
        conToken(2L);
        enLaBase(2, "HABILITADO", null, null);

        usuarioActual.validarSesion();
    }

    @Test
    void despuesDeCerrarSesionElTokenAnteriorSeRechaza() {
        conToken(2L);
        enLaBase(3, "HABILITADO", null, null);

        rechazaCon(usuarioActual::validarSesion, "SESION_CERRADA", HttpStatus.UNAUTHORIZED);
    }

    @Test
    void unTokenSinVersionSeRechaza() {
        conToken(null);
        enLaBase(0, "HABILITADO", null, null);

        rechazaCon(usuarioActual::validarSesion, "SESION_CERRADA", HttpStatus.UNAUTHORIZED);
    }

    @Test
    void cuentaSuspendidaConTokenVigenteSeRechaza() {
        conToken(0L);
        enLaBase(0, "SUSPENDIDO", null, 5L);

        rechazaCon(usuarioActual::validarSesion, "CUENTA_SUSPENDIDA", HttpStatus.FORBIDDEN);
    }

    @Test
    void guardiaDeshabilitadoConTokenVigenteSeRechaza() {
        conToken(0L);
        enLaBase(0, "DESHABILITADO", 1L, null);

        rechazaCon(usuarioActual::validarSesion, "USUARIO_DESHABILITADO", HttpStatus.FORBIDDEN);
    }

    @Test
    void barrioInactivoConTokenVigenteSeRechaza() {
        conToken(0L);
        enLaBase(0, "BARRIO_INACTIVO", 1L, null);

        rechazaCon(usuarioActual::validarSesion, "BARRIO_INACTIVO", HttpStatus.FORBIDDEN);
    }

    @Test
    void operadorReemplazadoConTokenVigenteSeRechaza() {
        conToken(0L);
        enLaBase(0, "OPERADOR_SIN_BARRIO", null, null);

        rechazaCon(usuarioActual::validarSesion, "OPERADOR_SIN_BARRIO", HttpStatus.FORBIDDEN);
    }

    @Test
    void cuentaQueYaNoExisteSeRechazaComoSinSesion() {
        conToken(0L);
        when(usuarioService.datosDeSesion(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(usuarioActual::validarSesion).isInstanceOf(InsufficientAuthenticationException.class);
    }

    @Test
    void elBarrioSaleDeLaBaseNoDelPedido() {
        conToken(0L);
        enLaBase(0, "HABILITADO", 4L, null);

        assertThat(usuarioActual.barrioId()).isEqualTo(4L);
        assertThat(usuarioActual.id()).isEqualTo(USUARIO_ID);
    }

    @Test
    void sinBarrioSeRechaza() {
        conToken(0L);
        enLaBase(0, "HABILITADO", null, null);

        rechazaCon(usuarioActual::barrioId, "SIN_BARRIO", HttpStatus.FORBIDDEN);
    }

    @Test
    void perfilAprobadoSeDevuelve() {
        conToken(0L);
        enLaBase(0, "HABILITADO", null, 9L);

        assertThat(usuarioActual.perfilIdAprobado()).isEqualTo(9L);
    }

    @Test
    void perfilPendienteRechazadoOInexistenteSeRechaza() {
        conToken(0L);
        enLaBase(0, "HABILITADO", null, null);

        rechazaCon(usuarioActual::perfilIdAprobado, "PERFIL_NO_APROBADO", HttpStatus.FORBIDDEN);
    }

    @Test
    void laBaseSeConsultaUnaSolaVezPorPeticion() {
        conToken(0L);
        enLaBase(0, "HABILITADO", 4L, 9L);

        usuarioActual.validarSesion();
        usuarioActual.barrioId();
        usuarioActual.perfilIdAprobado();

        verify(usuarioService, times(1)).datosDeSesion(USUARIO_ID);
    }

    @Test
    void sinTokenNoHayUsuario() {
        assertThatThrownBy(usuarioActual::id).isInstanceOf(InsufficientAuthenticationException.class);
    }
}
