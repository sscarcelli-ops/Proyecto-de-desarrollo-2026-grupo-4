package ar.edu.grupo4.accesos.auth;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecuperoControllerTest {

    private final RecuperoService recuperoService = mock(RecuperoService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RecuperoController(recuperoService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    private org.springframework.test.web.servlet.ResultActions enviar(String ruta, String json) throws Exception {
        return mockMvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    @DisplayName("Pedir el enlace responde 204, exista o no el email")
    void solicitarResponde204() throws Exception {
        enviar("/api/auth/recupero", "{\"email\":\"nadie@seed.test\"}").andExpect(status().isNoContent());

        verify(recuperoService).solicitar("nadie@seed.test");
    }

    @Test
    @DisplayName("Pedir el enlace con un email mal escrito responde 400 y no busca nada")
    void solicitarConEmailInvalidoResponde400() throws Exception {
        enviar("/api/auth/recupero", "{\"email\":\"no-es-un-email\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(recuperoService, never()).solicitar(anyString());
    }

    @Test
    @DisplayName("Un enlace que no sirve responde 422 con el mensaje de la US")
    void validarEnlaceInvalidoResponde422() throws Exception {
        doThrow(new BusinessException("ENLACE_INVALIDO", RecuperoService.ENLACE_INVALIDO))
            .when(recuperoService).validar("viejo");

        enviar("/api/auth/recupero/validar", "{\"token\":\"viejo\"}")
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("ENLACE_INVALIDO"))
            .andExpect(jsonPath("$.detail").value(RecuperoService.ENLACE_INVALIDO));
    }

    @Test
    @DisplayName("Restablecer con una contraseña que no cumple la política responde 400 y no la cambia")
    void restablecerConPasswordDebilResponde400() throws Exception {
        enviar("/api/auth/recupero/restablecer", "{\"token\":\"abc\",\"password\":\"Prueba1\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(recuperoService, never()).restablecer(anyString(), anyString());
    }

    @Test
    @DisplayName("Restablecer con enlace y contraseña válidos responde 204")
    void restablecerResponde204() throws Exception {
        enviar("/api/auth/recupero/restablecer", "{\"token\":\"abc\",\"password\":\"Nueva1234\"}")
            .andExpect(status().isNoContent());

        verify(recuperoService).restablecer("abc", "Nueva1234");
    }
}
