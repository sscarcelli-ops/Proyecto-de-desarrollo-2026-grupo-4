package ar.edu.grupo4.accesos.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import java.util.Locale;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifica el formato estándar de error sin levantar la aplicación ni la base de datos. */
class GlobalExceptionHandlerTest {

    record AltaRequest(@NotBlank String nombre, @Email String email) {
    }

    @RestController
    static class PruebaController {

        @PostMapping("/test/alta")
        void alta(@Valid @RequestBody AltaRequest request) {
        }

        @GetMapping("/test/negocio")
        void negocio() {
            throw new BusinessException("BARRIO_SIN_OPERADOR", "No se puede activar un barrio sin Operador.");
        }

        @GetMapping("/test/no-existe")
        void noExiste() {
            throw new NotFoundException();
        }

        @GetMapping("/test/prohibido")
        void prohibido() {
            throw new AccessDeniedException("sin rol");
        }

        @GetMapping("/test/inesperado")
        void inesperado() {
            throw new IllegalStateException("detalle interno que no debe llegar al usuario");
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new PruebaController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .setLocaleResolver(new FixedLocaleResolver(Locale.forLanguageTag("es")))
            .build();
    }

    @Test
    void validacionDevuelveErroresPorCampoEnEspanol() throws Exception {
        mockMvc.perform(post("/test/alta")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"\", \"email\": \"no-es-email\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.detail").value("Hay datos inválidos en la solicitud."))
            .andExpect(jsonPath("$.timestamp").value(notNullValue()))
            .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("nombre", "email")))
            .andExpect(jsonPath("$.errors[?(@.field == 'nombre')].message").value("no debe estar vacío"));
    }

    @Test
    void jsonMalformadoDevuelve400() throws Exception {
        mockMvc.perform(post("/test/alta").contentType(MediaType.APPLICATION_JSON).content("{roto"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void reglaDeNegocioDevuelve422ConSuCodigo() throws Exception {
        mockMvc.perform(get("/test/negocio"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("BARRIO_SIN_OPERADOR"))
            .andExpect(jsonPath("$.detail").value("No se puede activar un barrio sin Operador."));
    }

    @Test
    void recursoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/test/no-existe"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void sinPermisoDevuelve403() throws Exception {
        mockMvc.perform(get("/test/prohibido"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void errorInesperadoNoExponeDetallesInternos() throws Exception {
        mockMvc.perform(get("/test/inesperado"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado. Intentá nuevamente más tarde."));
    }

    @Test
    void metodoNoPermitidoTambienLlevaCodigo() throws Exception {
        mockMvc.perform(post("/test/negocio"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
            .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }
}
