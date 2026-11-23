package ar.edu.grupo4.accesos.common.security;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Lo que hace cumplir la anotación @PasswordSegura (US-44) en los DTO que definen una contraseña. Las reglas y los
 * mensajes están en PoliticaPassword, para que este validador y los Services usen exactamente los mismos.
 */
public class PasswordSeguraValidator implements ConstraintValidator<PasswordSegura, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return rechazar(context, PoliticaPassword.MENSAJE_OBLIGATORIA);
        }
        if (PoliticaPassword.esDemasiadoLarga(password)) {
            // Mensaje propio: el de la US no explica el límite que impone bcrypt
            return rechazar(context, PoliticaPassword.MENSAJE_LARGO);
        }
        return PoliticaPassword.cumpleRequisitos(password);
    }

    private static boolean rechazar(ConstraintValidatorContext context, String mensaje) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(mensaje).addConstraintViolation();
        return false;
    }
}
