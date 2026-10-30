package ar.edu.grupo4.accesos.common.security;

import java.nio.charset.StandardCharsets;

/**
 * Política de contraseñas (US-44, RNF-09): una sola regla para registro, recupero, cambio, activación del
 * Administrador y alta de Operador y Guardia. En los DTO se usa con @PasswordSegura; esta clase queda para
 * validar en un Service si alguna vez hace falta fuera de un request.
 */
public final class PoliticaPassword {

    public static final int MINIMO = 8;

    /** bcrypt solo admite hasta 72 bytes: más largo lanza una excepción al guardar el hash. */
    public static final int MAXIMO_BYTES = 72;

    /** El mensaje de la US-44, igual en todos los flujos. */
    public static final String MENSAJE =
        "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número";

    public static final String MENSAJE_LARGO = "La contraseña no puede tener más de 72 caracteres";

    /** Campo vacío: "[Campo] es obligatorio", como el resto de los campos de las US. */
    public static final String MENSAJE_OBLIGATORIA = "La contraseña es obligatoria";

    private PoliticaPassword() {
    }

    /** Mínimo 8 caracteres, al menos una mayúscula, una minúscula y un número. */
    public static boolean cumpleRequisitos(String password) {
        return password != null
            && password.length() >= MINIMO
            && password.chars().anyMatch(Character::isUpperCase)
            && password.chars().anyMatch(Character::isLowerCase)
            && password.chars().anyMatch(Character::isDigit);
    }

    public static boolean esDemasiadoLarga(String password) {
        return password != null && password.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES;
    }
}
