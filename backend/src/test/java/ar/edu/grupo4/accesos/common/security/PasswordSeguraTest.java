package ar.edu.grupo4.accesos.common.security;

import ar.edu.grupo4.accesos.common.exception.GlobalExceptionHandler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US-44: la política de contraseñas, sola y dentro de un request real. */
class PasswordSeguraTest {

    record DefinirPasswordRequest(@PasswordSegura String password) {
    }

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<ConstraintViolation<DefinirPasswordRequest>> validar(String password) {
        return validator.validate(new DefinirPasswordRequest(password));
    }

    // CA-1: 8 o más caracteres, mayúscula, minúscula y número
    @ParameterizedTest
    @ValueSource(strings = {"Prueba123", "Abcdefg1", "MiClave2026!", "Ñandú2026", "Contraseña muy larga 1"})
    void unaContrasenaQueCumpleSeAcepta(String password) {
        assertThat(validar(password)).isEmpty();
    }

    // CA-2: incumple cualquiera de los requisitos → se rechaza con el mensaje de la US
    @ParameterizedTest
    @ValueSource(strings = {
        "Prueba1",       // 7 caracteres
        "prueba123",     // sin mayúscula
        "PRUEBA123",     // sin minúscula
        "PruebaClave",   // sin número
        "        ",      // solo espacios
    })
    void unaContrasenaQueNoCumpleSeRechazaConElMensajeDeLaUs(String password) {
        assertThat(validar(password))
            .singleElement()
            .extracting(ConstraintViolation::getMessage)
            .isEqualTo("La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número");
    }

    // Campo vacío: "[Campo] es obligatorio", como el resto de los campos (US-01)
    @ParameterizedTest
    @NullAndEmptySource
    void unaContrasenaVaciaSeRechazaComoObligatoria(String password) {
        assertThat(validar(password))
            .singleElement()
            .extracting(ConstraintViolation::getMessage)
            .isEqualTo("La contraseña es obligatoria");
    }

    @Test
    void masDe72BytesSeRechazaConSuPropioMensaje() {
        // bcrypt no puede guardar más de 72 bytes: sin este límite, el alta respondería 500
        assertThat(validar("Ab1" + "x".repeat(70)))
            .singleElement()
            .extracting(ConstraintViolation::getMessage)
            .isEqualTo("La contraseña no puede tener más de 72 caracteres");
    }

    @Test
    void exactamente72BytesSeAcepta() {
        assertThat(validar("Ab1" + "x".repeat(69))).isEmpty();
    }

    // CA-4: una contraseña débil enviada directo por API se rechaza igual (400 con el error en el campo)
    @RestController
    static class DefinirPasswordController {
        @PostMapping("/test/password")
        void definir(@Valid @RequestBody DefinirPasswordRequest request) {
        }
    }

    @Test
    void porApiUnaContrasenaDebilDa400ConElMensajeEnElCampo() throws Exception {
        LocalValidatorFactoryBean springValidator = new LocalValidatorFactoryBean();
        springValidator.afterPropertiesSet();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DefinirPasswordController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(springValidator)
            .build();

        mockMvc.perform(post("/test/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\": \"debil\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.errors[0].field").value("password"))
            .andExpect(jsonPath("$.errors[0].message")
                .value("La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número"));
    }
}
