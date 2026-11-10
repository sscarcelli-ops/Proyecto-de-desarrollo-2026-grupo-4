package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.GlobalExceptionHandler;
import ar.edu.grupo4.accesos.usuario.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MiBarrioControllerTest {

    private final BarrioService barrioService = mock(BarrioService.class);
    private final UsuarioActual usuarioActual = mock(UsuarioActual.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MiBarrioController(barrioService, usuarioActual))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("GET /api/barrio devuelve el barrio de la sesión")
    void obtenerDevuelveElBarrioDeLaSesion() throws Exception {
        Barrio barrio = new Barrio("Barrio Los Álamos", null, "Av. Siempre Viva 123");
        barrio.setId(1L);
        when(usuarioActual.barrioId()).thenReturn(1L);
        when(barrioService.obtener(1L)).thenReturn(barrio);

        mockMvc.perform(get("/api/barrio"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nombre").value("Barrio Los Álamos"));
    }

    @Test
    @DisplayName("GET /api/barrio sin barrio en la sesión devuelve 403 y no busca ningún barrio")
    void obtenerSinBarrioDevuelve403() throws Exception {
        when(usuarioActual.barrioId())
            .thenThrow(new BusinessException("SIN_BARRIO", "No tenés un barrio asignado.", HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/api/barrio"))
            .andExpect(status().isForbidden());

        verify(barrioService, never()).obtener(anyLong());
    }
}
