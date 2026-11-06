package ar.edu.grupo4.accesos.trabajador;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.common.storage.FileStorageService;
import ar.edu.grupo4.accesos.trabajador.dto.DocumentoRequest;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorEditarRequest;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorRequest;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorResponse;
import ar.edu.grupo4.accesos.usuario.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TrabajadorServiceTest {

    private final TrabajadorRepository trabajadorRepository = mock(TrabajadorRepository.class);
    private final DocumentoRepository documentoRepository = mock(DocumentoRepository.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final UsuarioActual usuarioActual = mock(UsuarioActual.class);

    private TrabajadorService service;

    private static final Long PERFIL_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new TrabajadorService(trabajadorRepository, documentoRepository, fileStorageService, usuarioActual);
        when(usuarioActual.perfilIdAprobado()).thenReturn(PERFIL_ID);
    }

    @Test
    @DisplayName("US-25: Registrar trabajador exitosamente con estado PENDIENTE_CARGA")
    void crearTrabajadorExitoso() {
        TrabajadorRequest request = new TrabajadorRequest("Lucas", "Martínez", "35123456");
        when(trabajadorRepository.existsByPerfilIdAndDniIgnoreCase(PERFIL_ID, "35123456")).thenReturn(false);

        Trabajador guardado = new Trabajador();
        guardado.setId(1L);
        guardado.setPerfilId(PERFIL_ID);
        guardado.setNombre("Lucas");
        guardado.setApellido("Martínez");
        guardado.setDni("35123456");
        guardado.setDocumentos(new ArrayList<>());
        when(trabajadorRepository.save(any(Trabajador.class))).thenReturn(guardado);

        TrabajadorResponse response = service.crear(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nombre()).isEqualTo("Lucas");
        assertThat(response.apellido()).isEqualTo("Martínez");
        assertThat(response.dni()).isEqualTo("35123456");
        assertThat(response.estadoDocumental()).isEqualTo("PENDIENTE_CARGA");
        verify(trabajadorRepository).save(any(Trabajador.class));
    }

    @Test
    @DisplayName("US-25 / RN-11: Rechaza registro si el DNI ya existe en la misma empresa (TRABAJADOR_DUPLICADO)")
    void crearTrabajadorDniDuplicadoEnMismaEmpresaLanzaError() {
        TrabajadorRequest request = new TrabajadorRequest("Ana", "Vigente", "30777888");
        when(trabajadorRepository.existsByPerfilIdAndDniIgnoreCase(PERFIL_ID, "30777888")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Este trabajador ya está registrado en tu empresa");

        verify(trabajadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-26 / RF-23: Aislamiento entre empresas. Si se pide un trabajador ajeno, devuelve 404")
    void obtenerTrabajadorDeOtraEmpresaDevuelve404() {
        Long trabajadorAjenoId = 99L;
        when(trabajadorRepository.findByIdAndPerfilId(trabajadorAjenoId, PERFIL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(trabajadorAjenoId))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("US-26: Modificar nombre y apellido exitosamente")
    void editarTrabajadorModificaNombreYApellido() {
        Long trabajadorId = 5L;
        Trabajador existente = new Trabajador();
        existente.setId(trabajadorId);
        existente.setPerfilId(PERFIL_ID);
        existente.setNombre("Carlos");
        existente.setApellido("Vencido");
        existente.setDni("28999000");
        existente.setDocumentos(new ArrayList<>());

        when(trabajadorRepository.findByIdAndPerfilId(trabajadorId, PERFIL_ID)).thenReturn(Optional.of(existente));

        TrabajadorEditarRequest request = new TrabajadorEditarRequest("Carlos Alberto", "Vencido Modificado");
        TrabajadorResponse response = service.editar(trabajadorId, request);

        assertThat(response.nombre()).isEqualTo("Carlos Alberto");
        assertThat(response.apellido()).isEqualTo("Vencido Modificado");
        assertThat(existente.getNombre()).isEqualTo("Carlos Alberto");
        assertThat(existente.getApellido()).isEqualTo("Vencido Modificado");
    }

    @Test
    @DisplayName("Cálculo de estado documental: sin documentos es PENDIENTE_CARGA")
    void calculoEstadoSinDocumentosEsPendiente() {
        Trabajador t = new Trabajador();
        t.setDocumentos(new ArrayList<>());

        assertThat(service.calcularEstadoDocumental(t)).isEqualTo("PENDIENTE_CARGA");
    }

    @Test
    @DisplayName("Cálculo de estado documental: con algún vencido es VENCIDO")
    void calculoEstadoConVencidoEsVencido() {
        Trabajador t = new Trabajador();
        Documento d1 = new Documento();
        d1.setTipo(TipoDocumento.ART);
        d1.setFechaVencimiento(LocalDate.now().plusMonths(1));

        Documento d2 = new Documento();
        d2.setTipo(TipoDocumento.SEGURO_VIDA);
        d2.setFechaVencimiento(LocalDate.now().minusDays(1)); // Vencido

        ArrayList<Documento> docs = new ArrayList<>();
        docs.add(d1);
        docs.add(d2);
        t.setDocumentos(docs);

        assertThat(service.calcularEstadoDocumental(t)).isEqualTo("VENCIDO");
    }

    @Test
    @DisplayName("Cálculo de estado documental: ambos vigentes es VIGENTE")
    void calculoEstadoTodosVigentesEsVigente() {
        Trabajador t = new Trabajador();
        Documento d1 = new Documento();
        d1.setTipo(TipoDocumento.ART);
        d1.setFechaVencimiento(LocalDate.now().plusDays(10));

        Documento d2 = new Documento();
        d2.setTipo(TipoDocumento.SEGURO_VIDA);
        d2.setFechaVencimiento(LocalDate.now().plusMonths(2));

        ArrayList<Documento> docs = new ArrayList<>();
        docs.add(d1);
        docs.add(d2);
        t.setDocumentos(docs);

        assertThat(service.calcularEstadoDocumental(t)).isEqualTo("VIGENTE");
    }

    @Test
    @DisplayName("Cálculo de estado documental: un solo documento vigente es PENDIENTE_CARGA por faltar el otro requisito")
    void calculoEstadoUnSoloDocumentoVigenteEsPendiente() {
        Trabajador t = new Trabajador();
        Documento d1 = new Documento();
        d1.setTipo(TipoDocumento.ART);
        d1.setFechaVencimiento(LocalDate.now().plusDays(10));

        ArrayList<Documento> docs = new ArrayList<>();
        docs.add(d1);
        t.setDocumentos(docs);

        assertThat(service.calcularEstadoDocumental(t)).isEqualTo("PENDIENTE_CARGA");
    }

    @Test
    @DisplayName("Cálculo de estado documental: un solo documento pero vencido es VENCIDO")
    void calculoEstadoUnSoloDocumentoVencidoEsVencido() {
        Trabajador t = new Trabajador();
        Documento d1 = new Documento();
        d1.setTipo(TipoDocumento.ART);
        d1.setFechaVencimiento(LocalDate.now().minusDays(1));

        ArrayList<Documento> docs = new ArrayList<>();
        docs.add(d1);
        t.setDocumentos(docs);

        assertThat(service.calcularEstadoDocumental(t)).isEqualTo("VENCIDO");
    }

    @Test
    @DisplayName("US-28: Cargar documento por primera vez guarda archivo y persiste entidad")
    void cargarDocumentoNuevoExitoso() {
        Long trabajadorId = 1L;
        Trabajador trabajador = new Trabajador();
        trabajador.setId(trabajadorId);
        trabajador.setPerfilId(PERFIL_ID);
        trabajador.setDocumentos(new ArrayList<>());

        when(trabajadorRepository.findByIdAndPerfilId(trabajadorId, PERFIL_ID)).thenReturn(Optional.of(trabajador));
        when(documentoRepository.findByTrabajadorIdAndTipo(trabajadorId, TipoDocumento.ART)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("archivo", "art.pdf", "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46});
        DocumentoRequest request = new DocumentoRequest(TipoDocumento.ART, file, LocalDate.now().plusMonths(3));

        String rutaGuardada = "trabajadores/1/documentos/uuid-art.pdf";
        when(fileStorageService.guardar(eq(file), anyString())).thenReturn(rutaGuardada);

        TrabajadorResponse response = service.cargarDocumento(trabajadorId, request);

        verify(fileStorageService).validarPdf(file);
        verify(documentoRepository).saveAndFlush(any(Documento.class));
        assertThat(response.documentos()).hasSize(1);
        assertThat(response.documentos().get(0).tipo()).isEqualTo(TipoDocumento.ART);
        assertThat(response.documentos().get(0).archivoNombre()).isEqualTo("uuid-art.pdf");
        assertThat(response.estadoDocumental()).isEqualTo("PENDIENTE_CARGA");
    }

    @Test
    @DisplayName("US-28: Renovar documento reemplaza archivo en disco y elimina el anterior")
    void renovarDocumentoReemplazaArchivoViejo() {
        Long trabajadorId = 2L;
        Trabajador trabajador = new Trabajador();
        trabajador.setId(trabajadorId);
        trabajador.setPerfilId(PERFIL_ID);

        Documento docArt = new Documento();
        docArt.setId(9L);
        docArt.setTipo(TipoDocumento.ART);
        docArt.setArchivoRuta("trabajadores/2/documentos/art.pdf");
        docArt.setFechaVencimiento(LocalDate.now().plusMonths(5));
        docArt.setTrabajador(trabajador);

        Documento docViejo = new Documento();
        docViejo.setId(10L);
        docViejo.setTipo(TipoDocumento.SEGURO_VIDA);
        docViejo.setArchivoRuta("trabajadores/2/documentos/viejo.pdf");
        docViejo.setFechaVencimiento(LocalDate.now().minusDays(5));
        docViejo.setTrabajador(trabajador);

        ArrayList<Documento> docs = new ArrayList<>();
        docs.add(docArt);
        docs.add(docViejo);
        trabajador.setDocumentos(docs);

        when(trabajadorRepository.findByIdAndPerfilId(trabajadorId, PERFIL_ID)).thenReturn(Optional.of(trabajador));
        when(documentoRepository.findByTrabajadorIdAndTipo(trabajadorId, TipoDocumento.SEGURO_VIDA)).thenReturn(Optional.of(docViejo));

        MockMultipartFile fileNuevo = new MockMultipartFile("archivo", "nuevo.pdf", "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46});
        DocumentoRequest request = new DocumentoRequest(TipoDocumento.SEGURO_VIDA, fileNuevo, LocalDate.now().plusMonths(6));

        String rutaNueva = "trabajadores/2/documentos/nuevo.pdf";
        when(fileStorageService.guardar(eq(fileNuevo), anyString())).thenReturn(rutaNueva);

        TrabajadorResponse response = service.cargarDocumento(trabajadorId, request);

        verify(fileStorageService).validarPdf(fileNuevo);
        verify(fileStorageService).eliminar("trabajadores/2/documentos/viejo.pdf");
        verify(documentoRepository).saveAndFlush(docViejo);
        assertThat(docViejo.getArchivoRuta()).isEqualTo(rutaNueva);
        assertThat(response.estadoDocumental()).isEqualTo("VIGENTE");
    }

    @Test
    @DisplayName("US-28: Rollback en carga de documento si falla el guardado en base de datos")
    void cargarDocumentoFallaBaseDeDatosEliminaArchivoNuevo() {
        Long trabajadorId = 3L;
        Trabajador trabajador = new Trabajador();
        trabajador.setId(trabajadorId);
        trabajador.setPerfilId(PERFIL_ID);
        trabajador.setDocumentos(new ArrayList<>());

        when(trabajadorRepository.findByIdAndPerfilId(trabajadorId, PERFIL_ID)).thenReturn(Optional.of(trabajador));
        when(documentoRepository.findByTrabajadorIdAndTipo(trabajadorId, TipoDocumento.ART)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("archivo", "art.pdf", "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46});
        DocumentoRequest request = new DocumentoRequest(TipoDocumento.ART, file, LocalDate.now().plusMonths(3));

        String rutaGuardada = "trabajadores/3/documentos/uuid.pdf";
        when(fileStorageService.guardar(eq(file), anyString())).thenReturn(rutaGuardada);
        when(documentoRepository.saveAndFlush(any(Documento.class))).thenThrow(new RuntimeException("Error BD"));

        assertThatThrownBy(() -> service.cargarDocumento(trabajadorId, request))
            .isInstanceOf(RuntimeException.class);

        verify(fileStorageService).eliminar(rutaGuardada);
    }

    @Test
    @DisplayName("US-28: Eliminar documento remueve de la BD y borra archivo de disco")
    void eliminarDocumentoExitoso() {
        Long trabajadorId = 4L;
        Trabajador trabajador = new Trabajador();
        trabajador.setId(trabajadorId);
        trabajador.setPerfilId(PERFIL_ID);

        Documento doc = new Documento();
        doc.setId(20L);
        doc.setTipo(TipoDocumento.ART);
        String ruta = "trabajadores/4/documentos/art.pdf";
        doc.setArchivoRuta(ruta);

        ArrayList<Documento> docs = new ArrayList<>();
        docs.add(doc);
        trabajador.setDocumentos(docs);

        when(trabajadorRepository.findByIdAndPerfilId(trabajadorId, PERFIL_ID)).thenReturn(Optional.of(trabajador));
        when(documentoRepository.findByTrabajadorIdAndTipo(trabajadorId, TipoDocumento.ART)).thenReturn(Optional.of(doc));

        TrabajadorResponse response = service.eliminarDocumento(trabajadorId, TipoDocumento.ART);

        verify(documentoRepository).delete(doc);
        verify(documentoRepository).flush();
        verify(fileStorageService).eliminar(ruta);
        assertThat(response.estadoDocumental()).isEqualTo("PENDIENTE_CARGA");
        assertThat(response.documentos()).isEmpty();
    }

    @Test
    @DisplayName("US-25 / US-26: Cargar foto exitosamente actualiza fotoRuta y tieneFoto pasa a true")
    void cargarFotoExitoso() {
        Long trabajadorId = 5L;
        Trabajador trabajador = new Trabajador();
        trabajador.setId(trabajadorId);
        trabajador.setPerfilId(PERFIL_ID);
        trabajador.setDocumentos(new ArrayList<>());

        when(trabajadorRepository.findByIdAndPerfilId(trabajadorId, PERFIL_ID)).thenReturn(Optional.of(trabajador));

        MockMultipartFile foto = new MockMultipartFile("foto", "perfil.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        String rutaFoto = "trabajadores/5/foto/uuid.jpg";
        when(fileStorageService.guardar(eq(foto), anyString())).thenReturn(rutaFoto);

        TrabajadorResponse response = service.cargarFoto(trabajadorId, foto);

        verify(fileStorageService).validarFoto(foto);
        verify(trabajadorRepository).saveAndFlush(trabajador);
        assertThat(response.tieneFoto()).isTrue();
        assertThat(response.fotoUrl()).isEqualTo("/api/trabajadores/5/foto");
    }

    @Test
    @DisplayName("US-26: Cargar foto reemplazando una existente borra la anterior del disco")
    void cargarFotoReemplazaFotoExistenteYEliminaArchivoAnterior() {
        Long trabajadorId = 55L;
        Trabajador trabajador = new Trabajador();
        trabajador.setId(trabajadorId);
        trabajador.setPerfilId(PERFIL_ID);
        trabajador.setDocumentos(new ArrayList<>());
        String rutaVieja = "trabajadores/55/foto/anterior.jpg";
        trabajador.setFotoRuta(rutaVieja);

        when(trabajadorRepository.findByIdAndPerfilId(trabajadorId, PERFIL_ID)).thenReturn(Optional.of(trabajador));

        MockMultipartFile fotoNueva = new MockMultipartFile("foto", "nueva.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        String rutaNueva = "trabajadores/55/foto/nueva.jpg";
        when(fileStorageService.guardar(eq(fotoNueva), anyString())).thenReturn(rutaNueva);

        TrabajadorResponse response = service.cargarFoto(trabajadorId, fotoNueva);

        verify(fileStorageService).validarFoto(fotoNueva);
        verify(fileStorageService).eliminar(rutaVieja);
        verify(trabajadorRepository).saveAndFlush(trabajador);
        assertThat(response.tieneFoto()).isTrue();
        assertThat(trabajador.getFotoRuta()).isEqualTo(rutaNueva);
    }

    @Test
    @DisplayName("US-26: Eliminar foto borra archivo de disco y setea tieneFoto en false")
    void eliminarFotoExitoso() {
        Long trabajadorId = 6L;
        Trabajador trabajador = new Trabajador();
        trabajador.setId(trabajadorId);
        trabajador.setPerfilId(PERFIL_ID);
        trabajador.setDocumentos(new ArrayList<>());
        trabajador.setFotoRuta("trabajadores/6/foto/foto.jpg");

        when(trabajadorRepository.findByIdAndPerfilId(trabajadorId, PERFIL_ID)).thenReturn(Optional.of(trabajador));

        TrabajadorResponse response = service.eliminarFoto(trabajadorId);

        verify(fileStorageService).eliminar("trabajadores/6/foto/foto.jpg");
        verify(trabajadorRepository).saveAndFlush(trabajador);
        assertThat(response.tieneFoto()).isFalse();
        assertThat(response.fotoUrl()).isNull();
    }

    @Test
    @DisplayName("US-26: Descargar foto lanza NotFoundException si el trabajador no tiene foto")
    void descargarFotoSinFotoLanza404() {
        Long trabajadorId = 7L;
        Trabajador trabajador = new Trabajador();
        trabajador.setId(trabajadorId);
        trabajador.setPerfilId(PERFIL_ID);
        trabajador.setFotoRuta(null);

        when(trabajadorRepository.findByIdAndPerfilId(trabajadorId, PERFIL_ID)).thenReturn(Optional.of(trabajador));

        assertThatThrownBy(() -> service.descargarFoto(trabajadorId))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("US-26: Descargar foto con foto existente devuelve el recurso")
    void descargarFotoConFotoExistenteDevuelveRecurso() {
        Long trabajadorId = 77L;
        Trabajador trabajador = new Trabajador();
        trabajador.setId(trabajadorId);
        trabajador.setPerfilId(PERFIL_ID);
        String rutaFoto = "trabajadores/77/foto/foto.jpg";
        trabajador.setFotoRuta(rutaFoto);

        when(trabajadorRepository.findByIdAndPerfilId(trabajadorId, PERFIL_ID)).thenReturn(Optional.of(trabajador));
        Resource mockResource = new ByteArrayResource(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        when(fileStorageService.cargar(rutaFoto)).thenReturn(mockResource);

        Resource resultado = service.descargarFoto(trabajadorId);

        assertThat(resultado).isSameAs(mockResource);
    }

    @Test
    @DisplayName("US-26: Descargar documento devuelve el recurso de almacenamiento")
    void descargarDocumentoExitoso() {
        Long trabajadorId = 8L;
        Trabajador trabajador = new Trabajador();
        trabajador.setId(trabajadorId);
        trabajador.setPerfilId(PERFIL_ID);

        Documento doc = new Documento();
        doc.setArchivoRuta("trabajadores/8/documentos/art.pdf");
        doc.setTipo(TipoDocumento.ART);

        when(trabajadorRepository.findByIdAndPerfilId(trabajadorId, PERFIL_ID)).thenReturn(Optional.of(trabajador));
        when(documentoRepository.findByTrabajadorIdAndTipo(trabajadorId, TipoDocumento.ART)).thenReturn(Optional.of(doc));

        Resource mockResource = new ByteArrayResource(new byte[]{1, 2, 3});
        when(fileStorageService.cargar("trabajadores/8/documentos/art.pdf")).thenReturn(mockResource);

        Resource resultado = service.descargarDocumento(trabajadorId, TipoDocumento.ART);

        assertThat(resultado).isSameAs(mockResource);
    }
}
