package ar.edu.grupo4.accesos.common.security;

import ar.edu.grupo4.accesos.common.security.TokenService.TokenEmitido;
import ar.edu.grupo4.accesos.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Emite y valida tokens reales (sin Spring ni base de datos). */
class TokenServiceTest {

    private static final String SECRETO = "clave-de-prueba-de-al-menos-32-caracteres!!";
    private final JwtConfig jwtConfig = new JwtConfig();

    private static JwtProperties propiedades(String secreto, long segundos) {
        JwtProperties p = new JwtProperties();
        p.setSecret(secreto);
        p.setExpirationSeconds(segundos);
        return p;
    }

    private static Usuario admin() {
        Usuario u = new Usuario();
        u.setId(7L);
        u.setEmail("admin@seed.test");
        return u;
    }

    private TokenEmitido emitir(JwtProperties p) {
        return new TokenService(jwtConfig.jwtEncoder(p), p).emitir(admin());
    }

    @Test
    void elTokenEmitidoSeValidaYTraeLosDatosDelUsuario() {
        JwtProperties p = propiedades(SECRETO, 28800);
        TokenEmitido emitido = emitir(p);

        Jwt jwt = jwtConfig.jwtDecoder(p).decode(emitido.token());

        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("admin@seed.test");
        assertThat(jwt.getClaimAsStringList("roles")).isEqualTo(List.of());
        assertThat(jwt.getExpiresAt()).isEqualTo(emitido.expiraEn());
    }

    @Test
    void elTokenLlevaLaVersionDeSesionDelUsuario() {
        JwtProperties p = propiedades(SECRETO, 28800);
        Usuario usuario = admin();
        usuario.setVersionSesion(3);

        String token = new TokenService(jwtConfig.jwtEncoder(p), p).emitir(usuario).token();

        Number version = jwtConfig.jwtDecoder(p).decode(token).getClaim(TokenService.CLAIM_VERSION);
        assertThat(version.intValue()).isEqualTo(3);
    }

    @Test
    void unTokenAlteradoSeRechaza() {
        JwtProperties p = propiedades(SECRETO, 28800);
        String token = emitir(p).token();
        String alterado = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThatThrownBy(() -> jwtConfig.jwtDecoder(p).decode(alterado)).isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenFirmadoConOtraClaveSeRechaza() {
        String token = emitir(propiedades("otra-clave-distinta-de-al-menos-32-caracteres", 28800)).token();

        JwtDecoder decoder = jwtConfig.jwtDecoder(propiedades(SECRETO, 28800));
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenVencidoSeRechaza() {
        // Emitido hace 2 horas y vencido hace 1 (la validación tolera 60 segundos de diferencia de reloj)
        JwtProperties p = propiedades(SECRETO, 28800);
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(JwtConfig.ISSUER)
            .subject("7")
            .issuedAt(ahora.minus(2, ChronoUnit.HOURS))
            .expiresAt(ahora.minus(1, ChronoUnit.HOURS))
            .build();
        String vencido = jwtConfig.jwtEncoder(p)
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();

        assertThatThrownBy(() -> jwtConfig.jwtDecoder(p).decode(vencido)).isInstanceOf(JwtException.class);
    }

    @Test
    void unSecretoDeMenosDe32CaracteresNoSeAcepta() {
        assertThatThrownBy(() -> propiedades("corta", 28800).validate()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unaDuracionDeCeroONegativaNoSeAcepta() {
        assertThatThrownBy(() -> propiedades(SECRETO, 0).validate()).isInstanceOf(IllegalArgumentException.class);
    }
}
