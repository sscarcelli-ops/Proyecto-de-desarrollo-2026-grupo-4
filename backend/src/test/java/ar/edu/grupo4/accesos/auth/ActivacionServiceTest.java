package ar.edu.grupo4.accesos.auth;

import ar.edu.grupo4.accesos.auth.dto.ActivacionRequest;
import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.usuario.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivacionServiceTest {

    private static final String CODIGO = "GRUPO4-ACTIVAR-2026";
    private static final ActivacionRequest REQUEST = new ActivacionRequest(CODIGO, "admin@cliente.test", "Prueba123");

    private final TokenUnSoloUsoRepository tokenRepository = mock(TokenUnSoloUsoRepository.class);
    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private ActivacionService activacionService;

    @BeforeEach
    void setUp() {
        activacionService = new ActivacionService(tokenRepository, usuarioService);
        when(tokenRepository.findByTipoAndTokenHash(any(), anyString())).thenReturn(Optional.empty());
        when(tokenRepository.marcarUsado(any(), any())).thenReturn(1);
    }

    private TokenUnSoloUso guardado(Instant expiraEn, Instant usadoEn) {
        TokenUnSoloUso token = new TokenUnSoloUso();
        token.setId(7L);
        token.setTipo(TipoTokenUnSoloUso.ACTIVACION_ADMIN);
        token.setTokenHash(TokenUnSoloUso.hashDe(CODIGO));
        token.setExpiraEn(expiraEn);
        token.setUsadoEn(usadoEn);
        when(tokenRepository.findByTipoAndTokenHash(TipoTokenUnSoloUso.ACTIVACION_ADMIN, TokenUnSoloUso.hashDe(CODIGO)))
            .thenReturn(Optional.of(token));
        return token;
    }

    private TokenUnSoloUso vigente() {
        return guardado(Instant.now().plus(Duration.ofDays(1)), null);
    }

    // ---- Validar (pantalla "Activación de cuenta") ----

    @Test
    void codigoVigenteSeValidaSinConsumirse() {
        vigente();

        activacionService.validar(CODIGO);

        verify(tokenRepository, never()).marcarUsado(any(), any());
    }

    @Test
    void elCodigoNoDistingueMayusculasNiEspaciosAlrededor() {
        vigente();

        activacionService.validar("  grupo4-activar-2026 ");
    }

    @Test
    void codigoInexistenteDaElMensajeDeNoValidoOExpirado() {
        assertThatThrownBy(() -> activacionService.validar("CUALQUIER-COSA"))
            .isInstanceOf(BusinessException.class)
            .hasMessage(ActivacionService.TOKEN_INVALIDO)
            .extracting("code").isEqualTo("TOKEN_INVALIDO");
    }

    @Test
    void codigoVencidoDaElMismoMensajeQueUnoInexistente() {
        guardado(Instant.now().minus(Duration.ofMinutes(1)), null);

        assertThatThrownBy(() -> activacionService.validar(CODIGO))
            .hasMessage(ActivacionService.TOKEN_INVALIDO)
            .extracting("code").isEqualTo("TOKEN_INVALIDO");
    }

    @Test
    void codigoUsadoDaElMensajeDeYaUtilizado() {
        guardado(Instant.now().plus(Duration.ofDays(1)), Instant.now().minus(Duration.ofHours(1)));

        assertThatThrownBy(() -> activacionService.validar(CODIGO))
            .hasMessage(ActivacionService.TOKEN_UTILIZADO)
            .extracting("code").isEqualTo("TOKEN_UTILIZADO");
    }

    // ---- Activar (pantalla "Registrate") ----

    @Test
    void codigoVigenteCreaElAdministradorYConsumeElCodigo() {
        vigente();

        activacionService.activar(REQUEST);

        verify(tokenRepository).marcarUsado(eq(7L), any());
        verify(usuarioService).crearConRol("admin@cliente.test", "Prueba123", "ADMIN");
    }

    @Test
    void codigoUsadoNoCreaLaCuenta() {
        guardado(Instant.now().plus(Duration.ofDays(1)), Instant.now());

        assertThatThrownBy(() -> activacionService.activar(REQUEST)).hasMessage(ActivacionService.TOKEN_UTILIZADO);
        verify(usuarioService, never()).crearConRol(anyString(), anyString(), anyString());
    }

    @Test
    void siOtraPersonaLoUsoMientrasCompletabaElRegistroNoCreaLaCuenta() {
        // CA-6: al validar estaba libre, pero otro lo consumió antes de este UPDATE
        vigente();
        when(tokenRepository.marcarUsado(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> activacionService.activar(REQUEST))
            .hasMessage(ActivacionService.TOKEN_UTILIZADO)
            .extracting("code").isEqualTo("TOKEN_UTILIZADO");
        verify(usuarioService, never()).crearConRol(anyString(), anyString(), anyString());
    }

    @Test
    void emailYaRegistradoNoCreaLaCuentaNiConsumeElCodigo() {
        vigente();
        when(usuarioService.existePorEmail("admin@cliente.test")).thenReturn(true);

        assertThatThrownBy(() -> activacionService.activar(REQUEST))
            .hasMessage("Este email ya se encuentra registrado")
            .extracting("code").isEqualTo("EMAIL_DUPLICADO");
        verify(tokenRepository, never()).marcarUsado(any(), any());
        verify(usuarioService, never()).crearConRol(anyString(), anyString(), anyString());
    }

    // ---- Carga del código del .env al arrancar ----

    @Test
    void sinAdministradorCargaElCodigoConSuVencimientoYVenceLosAnteriores() {
        Instant antes = Instant.now();

        activacionService.cargarCodigoConfigurado(CODIGO, 30);

        verify(tokenRepository).vencerVigentes(eq(TipoTokenUnSoloUso.ACTIVACION_ADMIN), any());
        ArgumentCaptor<TokenUnSoloUso> guardado = ArgumentCaptor.forClass(TokenUnSoloUso.class);
        verify(tokenRepository).save(guardado.capture());
        assertThat(guardado.getValue().getTipo()).isEqualTo(TipoTokenUnSoloUso.ACTIVACION_ADMIN);
        assertThat(guardado.getValue().getTokenHash()).isEqualTo(TokenUnSoloUso.hashDe(CODIGO)).isNotEqualTo(CODIGO);
        assertThat(guardado.getValue().getUsuarioId()).isNull();
        assertThat(guardado.getValue().getExpiraEn()).isBetween(
            antes.plus(Duration.ofDays(30)), Instant.now().plus(Duration.ofDays(30)));
    }

    @Test
    void conAdministradorNoCargaNingunCodigo() {
        when(usuarioService.existeAdministrador()).thenReturn(true);

        activacionService.cargarCodigoConfigurado(CODIGO, 30);

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void elMismoCodigoNoSeVuelveACargarAunqueEsteVencido() {
        // Al reiniciar no se renueva: para uno nuevo, soporte cambia el valor del .env
        guardado(Instant.now().minus(Duration.ofDays(1)), null);

        activacionService.cargarCodigoConfigurado(CODIGO, 30);

        verify(tokenRepository, never()).save(any());
        verify(tokenRepository, never()).vencerVigentes(any(), any());
    }

    @Test
    void sinCodigoConfiguradoNoHaceNada() {
        activacionService.cargarCodigoConfigurado("  ", 30);

        verify(tokenRepository, never()).save(any());
    }
}
