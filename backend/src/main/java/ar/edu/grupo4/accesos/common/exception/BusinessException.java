package ar.edu.grupo4.accesos.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Violación de una regla de negocio. Se lanza desde los Services; el manejador global
 * la convierte en una respuesta de error estándar (ProblemDetail).
 *
 * <pre>throw new BusinessException("BARRIO_SIN_OPERADOR", "No se puede activar un barrio sin Operador.");</pre>
 *
 * El mensaje se muestra tal cual al usuario: escribirlo como en las historias de usuario.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, String message) {
        this(code, message, HttpStatus.UNPROCESSABLE_CONTENT);
    }

    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
