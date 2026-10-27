package ar.edu.grupo4.accesos.common.security;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * La contraseña cumple la política de US-44 (ver PoliticaPassword). Se usa en todo DTO que DEFINE una
 * contraseña (no en el login):
 *
 * <pre>public record AltaGuardiaRequest(@NotBlank @Email String email, @PasswordSegura String password) {}</pre>
 *
 * Una contraseña vacía o nula también se rechaza, con el mismo mensaje: no hace falta sumar @NotBlank.
 * Si no cumple, el backend responde 400 VALIDATION_ERROR con el mensaje en el campo (también si llega
 * directo por API, CA-4).
 */
@Documented
@Constraint(validatedBy = PasswordSeguraValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordSegura {

    String message() default PoliticaPassword.MENSAJE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
