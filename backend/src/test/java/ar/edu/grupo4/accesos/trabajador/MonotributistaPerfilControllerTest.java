package ar.edu.grupo4.accesos.trabajador;

import ar.edu.grupo4.accesos.common.exception.GlobalExceptionHandler;
import ar.edu.grupo4.accesos.perfil.PerfilService;
import ar.edu.grupo4.accesos.trabajador.dto.DocumentoResponse;
import ar.edu.grupo4.accesos.trabajador.dto.MonotributistaPerfilModificarRequest;
import ar.edu.grupo4.accesos.trabajador.dto.MonotributistaPerfilResponse;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorResponse;
import ar.edu.grupo4.accesos.usuario.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MonotributistaPerfilControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PerfilService perfilService;

    @Mock
    private TrabajadorService trabajadorService;

    @Mock
    private UsuarioActual usuarioActual;

    @InjectMocks
    private MonotributistaPerfilController controller;

    private MonotributistaPerfilResponse responseMock;
    private Trabajador trabajadorMock;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        responseMock = new MonotributistaPerfilResponse(
            10L, 20L, "Paula", "Gómez", "33222111", "27-33222111-1",
            "1133445566", "PENDIENTE_CARGA", false, null, List.of()
        );

        trabajadorMock = new Trabajador();
        trabajadorMock.setId(20L);
        trabajadorMock.setPerfilId(10L);
    }

    @Test
    @DisplayName("US-24 CA-1/CA-5: GET /api/monotributista/perfil devuelve los datos del perfil y trabajador")
    void obtenerPerfilDevuelveOk() throws Exception {
        when(usuarioActual.perfilIdAprobado()).thenReturn(10L);
        when(perfilService.obtenerPerfilMonotributista(10L)).thenReturn(responseMock);

        mockMvc.perform(get("/api/monotributista/perfil"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.perfilId").value(10))
            .andExpect(jsonPath("$.trabajadorId").value(20))
            .andExpect(jsonPath("$.nombre").value("Paula"))
            .andExpect(jsonPath("$.apellido").value("Gómez"))
            .andExpect(jsonPath("$.dni").value("33222111"))
            .andExpect(jsonPath("$.cuil").value("27-33222111-1"))
            .andExpect(jsonPath("$.telefono").value("1133445566"))
            .andExpect(jsonPath("$.estadoDocumental").value("PENDIENTE_CARGA"));

        verify(usuarioActual).perfilIdAprobado();
        verify(perfilService).obtenerPerfilMonotributista(10L);
    }

    @Test
    @DisplayName("US-24 CA-6: PUT /api/monotributista/perfil actualiza nombre, apellido y teléfono")
    void modificarPerfilDevuelveOk() throws Exception {
        when(usuarioActual.perfilIdAprobado()).thenReturn(10L);
        MonotributistaPerfilResponse actualizada = new MonotributistaPerfilResponse(
            10L, 20L, "Paula María", "Gómez Pérez", "33222111", "27-33222111-1",
            "1199887766", "PENDIENTE_CARGA", false, null, List.of()
        );
        when(perfilService.modificarPerfilMonotributista(eq(10L), any(MonotributistaPerfilModificarRequest.class)))
            .thenReturn(actualizada);

        String json = """
            {
                "nombre": "Paula María",
                "apellido": "Gómez Pérez",
                "telefono": "1199887766"
            }
            """;

        mockMvc.perform(put("/api/monotributista/perfil")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Paula María"))
            .andExpect(jsonPath("$.apellido").value("Gómez Pérez"))
            .andExpect(jsonPath("$.telefono").value("1199887766"));

        verify(perfilService).modificarPerfilMonotributista(eq(10L), any(MonotributistaPerfilModificarRequest.class));
    }

    @Test
    @DisplayName("US-24 CA-6: PUT rechaza nombre o apellido vacíos")
    void modificarPerfilValidaCamposObligatorios() throws Exception {
        String json = """
            {
                "nombre": "",
                "apellido": "",
                "telefono": "1122334455"
            }
            """;

        mockMvc.perform(put("/api/monotributista/perfil")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("US-24 CA-2: POST /api/monotributista/perfil/foto sube foto de perfil delegando al trabajador")
    void cargarFotoDevuelveOk() throws Exception {
        when(usuarioActual.perfilIdAprobado()).thenReturn(10L);
        when(trabajadorService.obtenerEntidadPorPerfilId(10L)).thenReturn(trabajadorMock);

        TrabajadorResponse tResp = new TrabajadorResponse(
            20L, "Paula", "Gómez", "33222111", "PENDIENTE_CARGA",
            true, false, List.of(), "/api/trabajadores/20/foto"
        );
        when(trabajadorService.cargarFoto(eq(20L), any())).thenReturn(tResp);

        MockMultipartFile file = new MockMultipartFile("foto", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/monotributista/perfil/foto").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tieneFoto").value(true));

        verify(trabajadorService).cargarFoto(eq(20L), any());
    }

    @Test
    @DisplayName("US-24 CA-2: DELETE /api/monotributista/perfil/foto elimina foto de perfil")
    void eliminarFotoDevuelveOk() throws Exception {
        when(usuarioActual.perfilIdAprobado()).thenReturn(10L);
        when(trabajadorService.obtenerEntidadPorPerfilId(10L)).thenReturn(trabajadorMock);

        TrabajadorResponse tResp = new TrabajadorResponse(
            20L, "Paula", "Gómez", "33222111", "PENDIENTE_CARGA",
            false, false, List.of(), null
        );
        when(trabajadorService.eliminarFoto(20L)).thenReturn(tResp);

        mockMvc.perform(delete("/api/monotributista/perfil/foto"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tieneFoto").value(false));

        verify(trabajadorService).eliminarFoto(20L);
    }

    @Test
    @DisplayName("US-24 CA-5: GET /api/monotributista/perfil/foto descarga foto")
    void descargarFotoDevuelveOk() throws Exception {
        when(usuarioActual.perfilIdAprobado()).thenReturn(10L);
        when(trabajadorService.obtenerEntidadPorPerfilId(10L)).thenReturn(trabajadorMock);

        Resource res = new ByteArrayResource(new byte[]{1, 2, 3}) {
            @Override
            public String getFilename() {
                return "foto.jpg";
            }
        };
        when(trabajadorService.descargarFoto(20L)).thenReturn(res);

        mockMvc.perform(get("/api/monotributista/perfil/foto"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/jpeg"));

        verify(trabajadorService).descargarFoto(20L);
    }

    @Test
    @DisplayName("US-24 CA-2: DELETE /api/monotributista/perfil/documentos/{tipo} elimina documento")
    void eliminarDocumentoDevuelveOk() throws Exception {
        when(usuarioActual.perfilIdAprobado()).thenReturn(10L);
        when(trabajadorService.obtenerEntidadPorPerfilId(10L)).thenReturn(trabajadorMock);

        TrabajadorResponse tResp = new TrabajadorResponse(
            20L, "Paula", "Gómez", "33222111", "PENDIENTE_CARGA",
            false, false, List.of(), null
        );
        when(trabajadorService.eliminarDocumento(20L, TipoDocumento.ART)).thenReturn(tResp);

        mockMvc.perform(delete("/api/monotributista/perfil/documentos/ART"))
            .andExpect(status().isOk());

        verify(trabajadorService).eliminarDocumento(20L, TipoDocumento.ART);
    }

    @Test
    @DisplayName("US-24 CA-5: GET /api/monotributista/perfil/documentos/{tipo} descarga PDF")
    void descargarDocumentoDevuelvePdf() throws Exception {
        when(usuarioActual.perfilIdAprobado()).thenReturn(10L);
        when(trabajadorService.obtenerEntidadPorPerfilId(10L)).thenReturn(trabajadorMock);

        Resource res = new ByteArrayResource(new byte[]{1, 2, 3});
        when(trabajadorService.descargarDocumento(20L, TipoDocumento.ART)).thenReturn(res);

        mockMvc.perform(get("/api/monotributista/perfil/documentos/ART"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"ART.pdf\""));

        verify(trabajadorService).descargarDocumento(20L, TipoDocumento.ART);
    }
}
