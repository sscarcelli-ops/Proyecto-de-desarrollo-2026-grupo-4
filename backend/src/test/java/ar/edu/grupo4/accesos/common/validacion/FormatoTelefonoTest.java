package ar.edu.grupo4.accesos.common.validacion;

import ar.edu.grupo4.accesos.barrio.dto.OperadorModificarRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** El formato del teléfono, probado con el DTO de modificar Operador (es la misma regla en los tres DTO). */
class FormatoTelefonoTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    private Set<ConstraintViolation<OperadorModificarRequest>> validar(String telefono) {
        return validator.validateProperty(new OperadorModificarRequest("Olga", "Operadora", telefono), "telefono");
    }

    @ParameterizedTest(name = "\"{0}\" es válido")
    @ValueSource(strings = {"1145550001", "+54 9 11 4555-0001", "(011) 4555-0001", " 1145559999 "})
    @DisplayName("Acepta números con espacios, +, - y paréntesis")
    void aceptaTelefonosValidos(String telefono) {
        assertThat(validar(telefono)).isEmpty();
    }

    @ParameterizedTest(name = "\"{0}\" no es válido")
    @ValueSource(strings = {"abc", "11-4555-ABCD", "telefono 1145550001", "12345", "------", "1145550001#"})
    @DisplayName("Rechaza letras, otros símbolos o menos de 6 números")
    void rechazaTelefonosInvalidos(String telefono) {
        assertThat(validar(telefono))
            .extracting(ConstraintViolation::getMessage)
            .contains(FormatoTelefono.MENSAJE);
    }

    @ParameterizedTest(name = "\"{0}\" solo da el error de obligatorio")
    @ValueSource(strings = {"", "   "})
    @DisplayName("Vacío no da el error de formato: si es obligatorio lo marca @NotBlank")
    void vacioNoDaErrorDeFormato(String telefono) {
        assertThat(validar(telefono))
            .extracting(ConstraintViolation::getMessage)
            .containsExactly("El teléfono es obligatorio");
    }
}
