package ar.edu.grupo4.accesos.perfil;

import ar.edu.grupo4.accesos.common.exception.GlobalExceptionHandler;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.perfil.dto.PerfilBarrioResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HabilitacionBarrioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HabilitacionBarrioService habilitacionBarrioService;

    @InjectMocks
    private HabilitacionBarrioController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    @Test
    @DisplayName("GET /api/barrio/perfiles devuelve 200 con el listado paginado y la habilitación de cada perfil")
    void listarDevuelveOk() throws Exception {
        PerfilBarrioResponse item = new PerfilBarrioResponse(
            10L, TipoPerfil.EMPRESA, "Limpiezas del Norte S.R.L.", "30-11223344-5", true);
        when(habilitacionBarrioService.listar(any(Pageable.class)))
            .thenReturn(new PageResponse<>(List.of(item), 0, 10, 1, 1));

        mockMvc.perform(get("/api/barrio/perfiles").param("page", "0").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].nombre").value("Limpiezas del Norte S.R.L."))
            .andExpect(jsonPath("$.content[0].habilitado").value(true));
    }

    @Test
    @DisplayName("GET /api/barrio/perfiles con ?sort= devuelve 400: el orden alfabético lo fija la consulta (RNF-08)")
    void listarConOrdenDevuelve400() throws Exception {
        mockMvc.perform(get("/api/barrio/perfiles").param("sort", "cuitCuil"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(habilitacionBarrioService);
    }

    @Test
    @DisplayName("POST /api/barrio/perfiles/{id}/habilitar devuelve 200 con el perfil habilitado")
    void habilitarDevuelveOk() throws Exception {
        when(habilitacionBarrioService.habilitar(10L)).thenReturn(new PerfilBarrioResponse(
            10L, TipoPerfil.EMPRESA, "Limpiezas del Norte S.R.L.", "30-11223344-5", true));

        mockMvc.perform(post("/api/barrio/perfiles/10/habilitar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.habilitado").value(true));
    }

    @Test
    @DisplayName("POST /api/barrio/perfiles/{id}/deshabilitar devuelve 200 con el perfil deshabilitado")
    void deshabilitarDevuelveOk() throws Exception {
        when(habilitacionBarrioService.deshabilitar(10L)).thenReturn(new PerfilBarrioResponse(
            10L, TipoPerfil.EMPRESA, "Limpiezas del Norte S.R.L.", "30-11223344-5", false));

        mockMvc.perform(post("/api/barrio/perfiles/10/deshabilitar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.habilitado").value(false));
    }

    @Test
    @DisplayName("POST /api/barrio/perfiles/{id}/habilitar de un perfil no aprobado o inexistente devuelve 404")
    void habilitarPerfilInexistenteDevuelve404() throws Exception {
        when(habilitacionBarrioService.habilitar(99L)).thenThrow(new NotFoundException());

        mockMvc.perform(post("/api/barrio/perfiles/99/habilitar"))
            .andExpect(status().isNotFound());
    }
}
