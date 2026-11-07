package ar.edu.grupo4.accesos.trabajador;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.common.storage.FileStorageService;
import ar.edu.grupo4.accesos.trabajador.dto.DocumentoRequest;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorEditarRequest;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorRequest;
import ar.edu.grupo4.accesos.usuario.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** US-41 y US-43: cada perfil opera solo sobre sus trabajadores, y solo si está aprobado. */
class TrabajadorAislamientoTest {

    private static final Long PERFIL_ID = 100L;
    private static final Long TRABAJADOR_AJENO_ID = 7L;

    private final TrabajadorRepository trabajadorRepository = mock(TrabajadorRepository.class);
    private final DocumentoRepository documentoRepository = mock(DocumentoRepository.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final UsuarioActual usuarioActual = mock(UsuarioActual.class);
    private TrabajadorService service;

    @BeforeEach
    void setUp() {
        service = new TrabajadorService(trabajadorRepository, documentoRepository, fileStorageService, usuarioActual);
        when(usuarioActual.perfilIdAprobado()).thenReturn(PERFIL_ID);
        when(trabajadorRepository.findByIdAndPerfilId(TRABAJADOR_AJENO_ID, PERFIL_ID)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("US-41 CA-1: el listado consulta solo el perfil de la sesión")
    void listarConsultaSoloElPerfilDeLaSesion() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Trabajador> vacia = new PageImpl<>(List.of(), pageable, 0);
        when(trabajadorRepository.findByPerfilId(PERFIL_ID, pageable)).thenReturn(vacia);

        service.listar(pageable);

        verify(trabajadorRepository).findByPerfilId(PERFIL_ID, pageable);
        verify(trabajadorRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("US-41 CA-2: un trabajador de otro perfil responde como inexistente")
    void consultarUnTrabajadorAjenoRespondeNoEncontrado() {
        assertThatThrownBy(() -> service.obtener(TRABAJADOR_AJENO_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("US-41 CA-2: un trabajador de otro perfil no se puede modificar")
    void modificarUnTrabajadorAjenoRespondeNoEncontrado() {
        TrabajadorEditarRequest cambios = new TrabajadorEditarRequest("Otro", "Nombre");

        assertThatThrownBy(() -> service.editar(TRABAJADOR_AJENO_ID, cambios)).isInstanceOf(NotFoundException.class);

        verify(trabajadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-41 CA-1 y CA-2: los documentos y la foto de un trabajador de otro perfil no se leen ni se tocan")
    void documentosYFotoDeUnTrabajadorAjenoRespondenNoEncontrado() {
        MockMultipartFile pdf = new MockMultipartFile("archivo", "art.pdf", "application/pdf", "%PDF-1.4".getBytes());
        MockMultipartFile foto = new MockMultipartFile("foto", "f.png", "image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G'});
        DocumentoRequest documento = new DocumentoRequest(TipoDocumento.ART, pdf, LocalDate.now().plusDays(30));

        assertThatThrownBy(() -> service.descargarDocumento(TRABAJADOR_AJENO_ID, TipoDocumento.ART))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.descargarFoto(TRABAJADOR_AJENO_ID)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.cargarDocumento(TRABAJADOR_AJENO_ID, documento)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.eliminarDocumento(TRABAJADOR_AJENO_ID, TipoDocumento.ART))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.cargarFoto(TRABAJADOR_AJENO_ID, foto)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.eliminarFoto(TRABAJADOR_AJENO_ID)).isInstanceOf(NotFoundException.class);

        // Ni la base de documentos ni el disco se usan: el perfil se controla antes
        verifyNoInteractions(documentoRepository, fileStorageService);
    }

    @Test
    @DisplayName("US-41 CA-3: el mismo DNI en otro perfil no impide cargarlo, y el registro queda en el perfil de la sesión")
    void elMismoDniEnOtroPerfilSeCargaEnElPerfilDeLaSesion() {
        when(trabajadorRepository.existsByPerfilIdAndDniIgnoreCase(PERFIL_ID, "30111222")).thenReturn(false);
        when(trabajadorRepository.save(any(Trabajador.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        service.crear(new TrabajadorRequest("Juan", "Pérez", "30111222"));

        ArgumentCaptor<Trabajador> guardado = ArgumentCaptor.forClass(Trabajador.class);
        verify(trabajadorRepository).save(guardado.capture());
        assertThat(guardado.getValue().getPerfilId()).isEqualTo(PERFIL_ID);
        verify(trabajadorRepository).existsByPerfilIdAndDniIgnoreCase(PERFIL_ID, "30111222");
    }

    @Test
    @DisplayName("US-43 CA-1: sin perfil aprobado no se opera y no se toca la base")
    void sinPerfilAprobadoNoSeOpera() {
        when(usuarioActual.perfilIdAprobado()).thenThrow(new BusinessException(
            "PERFIL_NO_APROBADO", "Tu perfil no está aprobado.", HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> service.listar(PageRequest.of(0, 10))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.obtener(1L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.editar(1L, new TrabajadorEditarRequest("A", "B")))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.crear(new TrabajadorRequest("A", "B", "30111222")))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.descargarDocumento(1L, TipoDocumento.ART)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.descargarFoto(1L)).isInstanceOf(BusinessException.class);

        verifyNoInteractions(trabajadorRepository, documentoRepository, fileStorageService);
    }
}
