package ar.edu.grupo4.accesos.common.security;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordSeguraValidator implements ConstraintValidator<PasswordSegura, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (PoliticaPassword.esDemasiadoLarga(password)) {
            // Mensaje propio: el de la US no explica el límite que impone bcrypt
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(PoliticaPassword.MENSAJE_LARGO).addConstraintViolation();
            return false;
        }
        return PoliticaPassword.cumpleRequisitos(password);
    }
}
