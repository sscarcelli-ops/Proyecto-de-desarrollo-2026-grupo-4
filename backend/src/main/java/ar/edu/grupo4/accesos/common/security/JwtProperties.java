package ar.edu.grupo4.accesos.common.security;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@ConfigurationProperties(prefix = "app.security.jwt")
@Getter
@Setter
public class JwtProperties {

    /** Firma HMAC: mínimo 32 caracteres. */
    private String secret;

    /** Expiración en segundos (28800s = 8 horas por defecto). */
    private long expirationSeconds = 28800;

    @PostConstruct
    public void validate() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                "La variable JWT_SECRET debe tener al menos 32 caracteres (256 bits) para HMAC-SHA256."
            );
        }
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("JWT_EXPIRATION_SECONDS debe ser mayor a 0.");
        }
    }
}