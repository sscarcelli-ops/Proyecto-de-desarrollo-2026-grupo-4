package ar.edu.grupo4.accesos.trabajador;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.GlobalExceptionHandler;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @DisplayName("US-27 / CA-5: POST /api/trabajadores/{id}/reincorporar reincorpora al trabajador y devuelve 200 con dadoDeBaja=false")
    void reincorporarDevuelveOk() throws Exception {
        TrabajadorResponse response = new TrabajadorResponse(
            42L, "Bruno", "Baja", "30999000",
            "VIGENTE", false, false, Collections.emptyList(), null
        );

        when(trabajadorService.reincorporar(42L)).thenReturn(response);

        mockMvc.perform(post("/api/trabajadores/42/reincorporar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(jsonPath("$.nombre").value("Bruno"))
            .andExpect(jsonPath("$.dadoDeBaja").value(false));

        verify(trabajadorService).reincorporar(42L);
    }

    @Test
    @DisplayName("US-27 / CA-7: POST /api/trabajadores/{id}/reincorporar si ya está activo devuelve 422")
    void reincorporarTrabajadorActivoDevuelve422() throws Exception {
        when(trabajadorService.reincorporar(42L))
            .thenThrow(new BusinessException("TRABAJADOR_NO_DADO_DE_BAJA", "El trabajador no se encuentra dado de baja."));

        mockMvc.perform(post("/api/trabajadores/42/reincorporar"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("TRABAJADOR_NO_DADO_DE_BAJA"))
            .andExpect(jsonPath("$.detail").value("El trabajador no se encuentra dado de baja."));

        verify(trabajadorService).reincorporar(42L);
    }

    @Test
    @DisplayName("US-27 / CA-6: POST /api/trabajadores/{id}/reincorporar de trabajador ajeno o inexistente devuelve 404")
    void reincorporarInexistenteDevuelve404() throws Exception {
        when(trabajadorService.reincorporar(999L))
            .thenThrow(new NotFoundException());

        mockMvc.perform(post("/api/trabajadores/999/reincorporar"))
            .andExpect(status().isNotFound());

        verify(trabajadorService).reincorporar(999L);
    }

    @Test
    @DisplayName("US-26 / US-29: GET /api/trabajadores sin parámetros devuelve 200 y pasa defaults al Service")
    void listarSinParametrosPasaDefaultsAlService() throws Exception {
        TrabajadorResponse item = new TrabajadorResponse(
            1L, "Ana", "Vigente", "30777888",
            "VIGENTE", false, false, Collections.emptyList(), null
        );
        PageResponse<TrabajadorResponse> pageResponse = PageResponse.from(
            new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1)
        );

        when(trabajadorService.listar(isNull(), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(pageResponse);

        mockMvc.perform(get("/api/trabajadores"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].nombre").value("Ana"))
            .andExpect(jsonPath("$.totalElements").value(1));

        verify(trabajadorService).listar(isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("US-29 / CA-1, CA-3: GET /api/trabajadores con filtros q, estadoDocumental e incluirBajas los propaga al Service")
    void listarConFiltrosPasaParametrosAlService() throws Exception {
        TrabajadorResponse item = new TrabajadorResponse(
            2L, "Juan", "Pérez", "30111222",
            "VENCIDO", false, true, Collections.emptyList(), null
        );
        PageResponse<TrabajadorResponse> pageResponse = PageResponse.from(
            new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1)
        );

        when(trabajadorService.listar(eq("perez"), eq("VENCIDO"), eq(true), any(Pageable.class)))
            .thenReturn(pageResponse);

        mockMvc.perform(get("/api/trabajadores")
                .param("q", "perez")
                .param("estadoDocumental", "VENCIDO")
                .param("incluirBajas", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].nombre").value("Juan"))
            .andExpect(jsonPath("$.content[0].estadoDocumental").value("VENCIDO"))
            .andExpect(jsonPath("$.content[0].dadoDeBaja").value(true));

        verify(trabajadorService).listar(eq("perez"), eq("VENCIDO"), eq(true), any(Pageable.class));
    }

    @Test
    @DisplayName("US-29: GET /api/trabajadores con estadoDocumental inválido devuelve 400 Bad Request")
    void listarConEstadoInvalidoDevuelve400() throws Exception {
        when(trabajadorService.listar(isNull(), eq("INVALIDO"), isNull(), any(Pageable.class)))
            .thenThrow(new BusinessException(
                "ESTADO_DOCUMENTAL_INVALIDO",
                "El estado documental debe ser VIGENTE, VENCIDO o PENDIENTE_CARGA",
                HttpStatus.BAD_REQUEST
            ));

        mockMvc.perform(get("/api/trabajadores")
                .param("estadoDocumental", "INVALIDO"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ESTADO_DOCUMENTAL_INVALIDO"));

        verify(trabajadorService).listar(isNull(), eq("INVALIDO"), isNull(), any(Pageable.class));
    }
}
