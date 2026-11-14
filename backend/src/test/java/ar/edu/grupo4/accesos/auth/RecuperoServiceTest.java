package ar.edu.grupo4.accesos.auth;

import ar.edu.grupo4.accesos.common.correo.CorreoService;
import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.usuario.Usuario;
import ar.edu.grupo4.accesos.usuario.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecuperoServiceTest {

    private static final String EMAIL = "guardia@seed.test";
    private static final String CODIGO = "a1b2c3";

    private final TokenUnSoloUsoRepository tokenRepository = mock(TokenUnSoloUsoRepository.class);
    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final CorreoService correoService = mock(CorreoService.class);
    private RecuperoService recuperoService;

    @BeforeEach
    void setUp() {
        recuperoService = new RecuperoService(tokenRepository, usuarioService, correoService,
            "http://localhost:5173/", 30);
        when(tokenRepository.findByTipoAndTokenHash(any(), anyString())).thenReturn(Optional.empty());
        when(tokenRepository.marcarUsado(any(), any())).thenReturn(1);
        when(correoService.enviar(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(9L);
        usuario.setEmail(EMAIL);
        when(usuarioService.buscarPorEmail(EMAIL)).thenReturn(Optional.of(usuario));
        return usuario;
    }

    private TokenUnSoloUso guardado(Instant expiraEn, Instant usadoEn) {
        TokenUnSoloUso token = new TokenUnSoloUso();
        token.setId(5L);
        token.setTipo(TipoTokenUnSoloUso.RECUPERO);
        token.setUsuarioId(9L);
        token.setTokenHash(TokenUnSoloUso.hashDe(CODIGO));
        token.setExpiraEn(expiraEn);
        token.setUsadoEn(usadoEn);
        when(tokenRepository.findByTipoAndTokenHash(TipoTokenUnSoloUso.RECUPERO, TokenUnSoloUso.hashDe(CODIGO)))
            .thenReturn(Optional.of(token));
        return token;
    }

    private TokenUnSoloUso vigente() {
        return guardado(Instant.now().plus(Duration.ofMinutes(10)), null);
    }

    // ---- Pedir el enlace ----

    @Test
    void conEmailRegistradoGuardaElHashYMandaElEnlaceAlEmail() {
        usuario();

        recuperoService.solicitar(EMAIL);

        ArgumentCaptor<TokenUnSoloUso> token = ArgumentCaptor.forClass(TokenUnSoloUso.class);
        verify(tokenRepository).save(token.capture());
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviar(eq(EMAIL), eq(RecuperoService.ASUNTO), cuerpo.capture(), html.capture());

        Matcher enlace = Pattern.compile("http://localhost:5173/restablecer-contrasena\\?token=([0-9a-f]{64})")
            .matcher(cuerpo.getValue());
        assertThat(enlace.find()).as("el correo trae el enlace con el código").isTrue();
        String codigo = enlace.group(1);
        // La versión HTML lleva el mismo enlace en el botón
        assertThat(html.getValue()).contains("href=\"" + enlace.group() + "\"").contains("Elegir una nueva contraseña");

        assertThat(token.getValue().getTipo()).isEqualTo(TipoTokenUnSoloUso.RECUPERO);
        assertThat(token.getValue().getUsuarioId()).isEqualTo(9L);
        // En la base queda el hash, nunca el código del enlace
        assertThat(token.getValue().getTokenHash()).isEqualTo(TokenUnSoloUso.hashDe(codigo)).isNotEqualTo(codigo);
        assertThat(token.getValue().getExpiraEn())
            .isBetween(Instant.now().plus(Duration.ofMinutes(29)), Instant.now().plus(Duration.ofMinutes(31)));
    }

    @Test
    void pedirOtroEnlaceVenceLosAnterioresDeEsaCuenta() {
        usuario();

        recuperoService.solicitar(EMAIL);

        verify(tokenRepository).vencerVigentesDeUsuario(eq(TipoTokenUnSoloUso.RECUPERO), eq(9L), any());
    }

    @Test
    void cadaPedidoGeneraUnCodigoDistinto() {
        usuario();

        recuperoService.solicitar(EMAIL);
        recuperoService.solicitar(EMAIL);

        ArgumentCaptor<TokenUnSoloUso> tokens = ArgumentCaptor.forClass(TokenUnSoloUso.class);
        verify(tokenRepository, times(2)).save(tokens.capture());
        assertThat(tokens.getAllValues().get(0).getTokenHash())
            .isNotEqualTo(tokens.getAllValues().get(1).getTokenHash());
    }

    @Test
    void conEmailNoRegistradoNoGuardaNiMandaNadaYNoFalla() {
        when(usuarioService.buscarPorEmail("nadie@seed.test")).thenReturn(Optional.empty());

        recuperoService.solicitar("nadie@seed.test");

        verify(tokenRepository, never()).save(any());
        verify(correoService, never()).enviar(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void siElCorreoNoSaleElPedidoNoFalla() {
        usuario();
        when(correoService.enviar(anyString(), anyString(), anyString(), anyString())).thenReturn(false);

        recuperoService.solicitar(EMAIL);

        verify(tokenRepository).save(any());
    }

    // ---- Abrir el enlace ----

    @Test
    void enlaceVigenteSeValidaSinConsumirse() {
        vigente();

        recuperoService.validar(CODIGO);

        verify(tokenRepository, never()).marcarUsado(any(), any());
    }

    @Test
    void enlaceInexistenteDaElError() {
        assertThatThrownBy(() -> recuperoService.validar("otro"))
            .isInstanceOf(BusinessException.class)
            .hasMessage(RecuperoService.ENLACE_INVALIDO)
            .extracting("code").isEqualTo("ENLACE_INVALIDO");
    }

    @Test
    void enlaceVencidoDaElMismoError() {
        guardado(Instant.now().minus(Duration.ofMinutes(1)), null);

        assertThatThrownBy(() -> recuperoService.validar(CODIGO)).hasMessage(RecuperoService.ENLACE_INVALIDO);
    }

    @Test
    void enlaceUsadoDaElMismoError() {
        guardado(Instant.now().plus(Duration.ofMinutes(10)), Instant.now().minus(Duration.ofMinutes(1)));

        assertThatThrownBy(() -> recuperoService.validar(CODIGO)).hasMessage(RecuperoService.ENLACE_INVALIDO);
    }

    // ---- Definir la contraseña nueva ----

    @Test
    void conEnlaceVigenteCambiaLaContrasenaYLoConsume() {
        vigente();

        recuperoService.restablecer(CODIGO, "Nueva1234");

        verify(tokenRepository).marcarUsado(eq(5L), any());
        verify(usuarioService).restablecerPassword(9L, "Nueva1234");
    }

    @Test
    void conEnlaceVencidoNoCambiaLaContrasena() {
        guardado(Instant.now().minus(Duration.ofMinutes(1)), null);

        assertThatThrownBy(() -> recuperoService.restablecer(CODIGO, "Nueva1234"))
            .hasMessage(RecuperoService.ENLACE_INVALIDO);
        verify(usuarioService, never()).restablecerPassword(anyLong(), anyString());
    }

    @Test
    void siOtroPedidoLoUsoRecienNoCambiaLaContrasena() {
        vigente();
        when(tokenRepository.marcarUsado(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> recuperoService.restablecer(CODIGO, "Nueva1234"))
            .hasMessage(RecuperoService.ENLACE_INVALIDO);
        verify(usuarioService, never()).restablecerPassword(anyLong(), anyString());
    }
}
