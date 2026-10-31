package ar.edu.grupo4.accesos.common.security;

import ar.edu.grupo4.accesos.usuario.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    /** Versión de sesión del usuario al emitir el token: si después cambia (cerró sesión), el token deja de valer. */
    public static final String CLAIM_VERSION = "ver";

    public record TokenEmitido(String token, Instant expiraEn) {
    }

    /**
     * Token de sesión: id del usuario (sub), email, roles y versión de sesión; vence a las 8 horas
     * (JWT_EXPIRATION_SECONDS).
     */
    public TokenEmitido emitir(Usuario usuario) {
        // En segundos enteros, como los guarda el token: así expiraEn coincide exactamente con su vencimiento
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiraEn = ahora.plusSeconds(jwtProperties.getExpirationSeconds());

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(JwtConfig.ISSUER)
            .issuedAt(ahora)
            .expiresAt(expiraEn)
            .subject(usuario.getId().toString())
            .claim("email", usuario.getEmail())
            .claim("roles", usuario.codigosDeRoles())
            .claim(CLAIM_VERSION, usuario.getVersionSesion())
            .build();

        // Sin este header el encoder busca una clave RSA (RS256 por defecto) y falla con la clave HMAC
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenEmitido(token, expiraEn);
    }
}
