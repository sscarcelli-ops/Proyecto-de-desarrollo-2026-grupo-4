package ar.edu.grupo4.accesos.trabajador;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.GlobalExceptionHandler;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TrabajadorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TrabajadorService trabajadorService;

    @InjectMocks
    private TrabajadorController trabajadorController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(trabajadorController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    @Test
    @DisplayName("US-27: POST /api/trabajadores/{id}/baja da de baja al trabajador y devuelve 200 con dadoDeBaja=true")
    void darDeBajaDevuelveOk() throws Exception {
        TrabajadorResponse response = new TrabajadorResponse(
            42L, "Carlos", "Vencido", "30333444",
            "VENCIDO", false, true, Collections.emptyList(), null
        );

        when(trabajadorService.darDeBaja(42L)).thenReturn(response);

        mockMvc.perform(post("/api/trabajadores/42/baja"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(jsonPath("$.nombre").value("Carlos"))
            .andExpect(jsonPath("$.dadoDeBaja").value(true));

        verify(trabajadorService).darDeBaja(42L);
    }

    @Test
    @DisplayName("US-27: POST /api/trabajadores/{id}/baja si ya está de baja devuelve 422 con TRABAJADOR_YA_DADO_DE_BAJA")
    void darDeBajaYaDadoDeBajaDevuelve422() throws Exception {
        when(trabajadorService.darDeBaja(42L))
            .thenThrow(new BusinessException("TRABAJADOR_YA_DADO_DE_BAJA", "El trabajador ya se encuentra dado de baja."));

        mockMvc.perform(post("/api/trabajadores/42/baja"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("TRABAJADOR_YA_DADO_DE_BAJA"))
            .andExpect(jsonPath("$.detail").value("El trabajador ya se encuentra dado de baja."));

        verify(trabajadorService).darDeBaja(42L);
    }

    @Test
    @DisplayName("US-27 / RF-23: POST /api/trabajadores/{id}/baja de trabajador ajeno o inexistente devuelve 404")
    void darDeBajaInexistenteDevuelve404() throws Exception {
        when(trabajadorService.darDeBaja(999L))
            .thenThrow(new NotFoundException());

        mockMvc.perform(post("/api/trabajadores/999/baja"))
            .andExpect(status().isNotFound());

        verify(trabajadorService).darDeBaja(999L);
    }

    @Test
    @DisplayName("US-27 / CA-5: PATCH /api/trabajadores/{id}/reincorporar reincorpora al trabajador y devuelve 200 con dadoDeBaja=false")
    void reincorporarDevuelveOk() throws Exception {
        TrabajadorResponse response = new TrabajadorResponse(
            42L, "Bruno", "Baja", "30999000",
            "VIGENTE", false, false, Collections.emptyList(), null
        );

        when(trabajadorService.reincorporar(42L)).thenReturn(response);

        mockMvc.perform(patch("/api/trabajadores/42/reincorporar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(jsonPath("$.nombre").value("Bruno"))
            .andExpect(jsonPath("$.dadoDeBaja").value(false));

        verify(trabajadorService).reincorporar(42L);
    }

    @Test
    @DisplayName("US-27 / CA-7: PATCH /api/trabajadores/{id}/reincorporar si ya está activo devuelve 422")
    void reincorporarTrabajadorActivoDevuelve422() throws Exception {
        when(trabajadorService.reincorporar(42L))
            .thenThrow(new BusinessException("TRABAJADOR_NO_DADO_DE_BAJA", "El trabajador no se encuentra dado de baja."));

        mockMvc.perform(patch("/api/trabajadores/42/reincorporar"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("TRABAJADOR_NO_DADO_DE_BAJA"))
            .andExpect(jsonPath("$.detail").value("El trabajador no se encuentra dado de baja."));

        verify(trabajadorService).reincorporar(42L);
    }

    @Test
    @DisplayName("US-27 / CA-6: PATCH /api/trabajadores/{id}/reincorporar de trabajador ajeno o inexistente devuelve 404")
    void reincorporarInexistenteDevuelve404() throws Exception {
        when(trabajadorService.reincorporar(999L))
            .thenThrow(new NotFoundException());

        mockMvc.perform(patch("/api/trabajadores/999/reincorporar"))
            .andExpect(status().isNotFound());

        verify(trabajadorService).reincorporar(999L);
    }
}
