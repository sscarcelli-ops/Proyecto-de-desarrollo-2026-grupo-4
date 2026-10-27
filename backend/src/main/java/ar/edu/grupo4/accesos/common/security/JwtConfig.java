package ar.edu.grupo4.accesos.common.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/** Emisor y validador de JWT firmados con HMAC-SHA256 y la clave JWT_SECRET (RNF-01). */
@Configuration
public class JwtConfig {

    /** Quién emite el token: se escribe al emitirlo y se exige al validarlo. */
    public static final String ISSUER = "accesos-grupo4";

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave(jwtProperties)));
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave(jwtProperties))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
        // Además de la firma: que no esté vencido y que lo haya emitido este backend
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }

    private static SecretKey clave(JwtProperties jwtProperties) {
        return new SecretKeySpec(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
