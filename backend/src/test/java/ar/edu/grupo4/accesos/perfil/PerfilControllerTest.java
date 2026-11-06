package ar.edu.grupo4.accesos.perfil;

import ar.edu.grupo4.accesos.common.exception.GlobalExceptionHandler;
import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.perfil.dto.PerfilResponse;
import ar.edu.grupo4.accesos.perfil.dto.RechazarPerfilRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PerfilControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PerfilService perfilService;

    @InjectMocks
    private PerfilController perfilController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(perfilController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    @Test
    @DisplayName("GET /api/perfiles devuelve 200 con listado paginado")
    void listarPerfilesDevuelveOk() throws Exception {
        PerfilResponse item = new PerfilResponse(
            1L, 10L, "empresa@test.com", TipoPerfil.EMPRESA, EstadoPerfil.PENDIENTE,
            "30-11223344-5", "Empresa Norte S.A.", null, null, null,
            "1144556677", null, OffsetDateTime.now()
        );
        PageResponse<PerfilResponse> pagina = new PageResponse<>(List.of(item), 0, 10, 1, 1);

        when(perfilService.listar(any(), any(), any(Pageable.class))).thenReturn(pagina);

        mockMvc.perform(get("/api/perfiles")
                .param("page", "0")
                .param("size", "10")
                .param("q", "Norte"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].razonSocial").value("Empresa Norte S.A."))
            .andExpect(jsonPath("$.content[0].cuitCuil").value("30-11223344-5"));
    }

    @Test
    @DisplayName("GET /api/perfiles/{id} devuelve 200 con detalle del perfil")
    void obtenerPorIdDevuelveOk() throws Exception {
        PerfilResponse detalle = new PerfilResponse(
            2L, 20L, "mono@test.com", TipoPerfil.MONOTRIBUTISTA, EstadoPerfil.PENDIENTE,
            "20-33445566-7", null, "Lucas", "Martínez", "33445566",
            "1122334455", null, OffsetDateTime.now()
        );

        when(perfilService.obtenerPorId(2L)).thenReturn(detalle);

        mockMvc.perform(get("/api/perfiles/2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.tipo").value("MONOTRIBUTISTA"))
            .andExpect(jsonPath("$.nombre").value("Lucas"))
            .andExpect(jsonPath("$.apellido").value("Martínez"));
    }

    @Test
    @DisplayName("GET /api/perfiles/{id}/constancia devuelve 200 con PDF y encabezado inline")
    void descargarConstanciaDevuelvePdf() throws Exception {
        byte[] pdfBytes = "%PDF-1.4 prueba constancia".getBytes();
        ByteArrayResource resource = new ByteArrayResource(pdfBytes);

        when(perfilService.obtenerConstancia(3L)).thenReturn(resource);

        mockMvc.perform(get("/api/perfiles/3/constancia"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"constancia-3.pdf\""))
            .andExpect(content().bytes(pdfBytes));
    }

    @Test
    @DisplayName("POST /api/perfiles/{id}/aprobar devuelve 200 con estado APROBADO")
    void aprobarPerfilDevuelveOk() throws Exception {
        PerfilResponse aprobado = new PerfilResponse(
            4L, 40L, "empresa@test.com", TipoPerfil.EMPRESA, EstadoPerfil.APROBADO,
            "30-11223344-5", "Empresa Aprobada", null, null, null,
            null, null, OffsetDateTime.now()
        );

        when(perfilService.aprobar(4L)).thenReturn(aprobado);

        mockMvc.perform(post("/api/perfiles/4/aprobar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("APROBADO"))
            .andExpect(jsonPath("$.id").value(4));

        verify(perfilService).aprobar(4L);
    }

    @Test
    @DisplayName("POST /api/perfiles/{id}/rechazar con motivo devuelve 200 con estado RECHAZADO")
    void rechazarPerfilConMotivoDevuelveOk() throws Exception {
        PerfilResponse rechazado = new PerfilResponse(
            5L, 50L, "mono@test.com", TipoPerfil.MONOTRIBUTISTA, EstadoPerfil.RECHAZADO,
            "20-33445566-7", null, "Pedro", "Gómez", "33445566",
            null, "Documentación incompleta", OffsetDateTime.now()
        );

        when(perfilService.rechazar(eq(5L), any(RechazarPerfilRequest.class))).thenReturn(rechazado);

        RechazarPerfilRequest request = new RechazarPerfilRequest("Documentación incompleta");

        mockMvc.perform(post("/api/perfiles/5/rechazar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("RECHAZADO"))
            .andExpect(jsonPath("$.motivoRechazo").value("Documentación incompleta"));
    }

    @Test
    @DisplayName("POST /api/perfiles/{id}/rechazar con motivo vacío devuelve 400 VALIDATION_ERROR")
    void rechazarPerfilSinMotivoDevuelveError() throws Exception {
        RechazarPerfilRequest request = new RechazarPerfilRequest("   ");

        mockMvc.perform(post("/api/perfiles/5/rechazar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.errors[0].field").value("motivo"))
            .andExpect(jsonPath("$.errors[0].message").value("El motivo de rechazo es obligatorio"));
    }
}
