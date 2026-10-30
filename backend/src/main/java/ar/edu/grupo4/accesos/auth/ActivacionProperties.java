package ar.edu.grupo4.accesos.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * US-01: código de activación del Administrador inicial, entregado en el manual de instalación.
 * Se configura en el .env (ACTIVACION_TOKEN) y se carga al arrancar (ActivacionInicializador).
 */
@Component
@ConfigurationProperties(prefix = "app.activacion")
@Getter
@Setter
public class ActivacionProperties {

    /** Código que se ingresa en "Activación de cuenta". Vacío: no se carga ninguno. */
    private String token;

    /** Días de vigencia desde que se carga (el primer arranque con ese código). */
    private int vigenciaDias = 30;
}
