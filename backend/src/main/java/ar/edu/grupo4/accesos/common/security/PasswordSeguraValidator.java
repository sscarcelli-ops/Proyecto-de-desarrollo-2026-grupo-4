package ar.edu.grupo4.accesos.common.security;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

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
