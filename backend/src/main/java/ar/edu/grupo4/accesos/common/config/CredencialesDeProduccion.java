package ar.edu.grupo4.accesos.common.config;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * T-12: en producción el backend no arranca con credenciales de ejemplo. Son públicas (están en el repositorio):
 * con el JWT_SECRET de ejemplo cualquiera podría firmar un token de Administrador.
 *
 * Se rechazan los valores de backend/.env.example, los "CAMBIAR-..." de docker/produccion.env.example y la contraseña
 * "postgres". El error nombra las variables, nunca sus valores.
 */
@Component
@Profile("prod")
public class CredencialesDeProduccion {

    // Propiedad de Spring -> variable de entorno que la define (la que hay que cambiar)
    private static final Map<String, String> CREDENCIALES = Map.of(
        "app.security.jwt.secret", "JWT_SECRET",
        "app.activacion.token", "ACTIVACION_TOKEN",
        "spring.datasource.password", "DB_PASSWORD"
    );

    private static final Set<String> VALORES_DE_EJEMPLO = Set.of(
        "tu_clave_secreta_super_segura_de_al_menos_256_bits",
        "GRUPO4-ACTIVAR-2026",
        "postgres"
    );

    public CredencialesDeProduccion(Environment environment) {
        List<String> sinCambiar = new ArrayList<>();
        CREDENCIALES.forEach((propiedad, variable) -> {
            String valor = environment.getProperty(propiedad, "");
            if (VALORES_DE_EJEMPLO.contains(valor) || valor.startsWith("CAMBIAR")) {
                sinCambiar.add(variable);
            }
        });
        if (!sinCambiar.isEmpty()) {
            throw new IllegalStateException("Producción con credenciales de ejemplo: cambiar "
                + String.join(", ", sinCambiar.stream().sorted().toList())
                + " en docker/produccion.env (ver docker/produccion.env.example)");
        }
    }
}
