package ar.edu.grupo4.accesos.common.validacion;

/**
 * Formato de un teléfono, el mismo en todos los DTO que lo reciben (Operador, solicitud de perfil):
 * <pre>&#64;Pattern(regexp = FormatoTelefono.REGEX, message = FormatoTelefono.MENSAJE)</pre>
 * Al menos 6 números y, además de números, solo espacios, +, - y paréntesis (ej. "+54 9 11 4555-0000"). Vacío pasa:
 * si el teléfono es obligatorio lo controla @NotBlank, y si es opcional, vacío es "sin teléfono". El largo máximo
 * lo controla @Size. El frontend usa la misma regla (shared/lib/telefono.ts).
 */
public final class FormatoTelefono {

    public static final String REGEX = "^\\s*$|^(?=(?:\\D*\\d){6})[0-9 +()-]+$";

    public static final String MENSAJE = "Ingresá un teléfono válido: solo números, espacios, +, - y paréntesis";

    private FormatoTelefono() {
    }
}
