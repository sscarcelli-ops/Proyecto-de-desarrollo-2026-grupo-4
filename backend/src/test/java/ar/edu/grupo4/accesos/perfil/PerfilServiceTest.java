package ar.edu.grupo4.accesos.perfil;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.common.storage.FileStorageService;
import ar.edu.grupo4.accesos.perfil.dto.PerfilResponse;
import ar.edu.grupo4.accesos.perfil.dto.RechazarPerfilRequest;
import ar.edu.grupo4.accesos.perfil.dto.SolicitarPerfilRequest;
import ar.edu.grupo4.accesos.usuario.Rol;
import ar.edu.grupo4.accesos.usuario.RolRepository;
import ar.edu.grupo4.accesos.usuario.Usuario;
import ar.edu.grupo4.accesos.usuario.UsuarioRepository;
import ar.edu.grupo4.accesos.usuario.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PerfilService perfilService;

    private Usuario usuario;
    private MockMultipartFile constanciaPdf;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setEmail("usuario@test.com");
        usuario.setPasswordHash("hashBcrypt");
        usuario.setId(10L);
        constanciaPdf = new MockMultipartFile(
            "constancia", "constancia.pdf", "application/pdf", "%PDF-1.4 prueba".getBytes()
        );
    }

    @Test
    @DisplayName("solicitar perfil Empresa con datos válidos crea solicitud Pendiente")
    void solicitarEmpresaValida() {
        when(usuarioService.buscarPorId(10L)).thenReturn(Optional.of(usuario));
        when(perfilRepository.existsByUsuarioId(10L)).thenReturn(false);
        when(perfilRepository.existsByCuitCuil("30-11223344-5")).thenReturn(false);
        when(fileStorageService.guardar(eq(constanciaPdf), any())).thenReturn("perfiles/10/constancia/uuid.pdf");
        when(perfilRepository.saveAndFlush(any(Perfil.class))).thenAnswer(inv -> {
            Perfil p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        SolicitarPerfilRequest req = new SolicitarPerfilRequest(
            TipoPerfil.EMPRESA,
            "30-11223344-5",
            "Mi Empresa S.A.",
            null, null, null, null,
            "1122334455",
            constanciaPdf
        );

        PerfilResponse res = perfilService.solicitar(10L, req);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.tipo()).isEqualTo(TipoPerfil.EMPRESA);
        assertThat(res.estado()).isEqualTo(EstadoPerfil.PENDIENTE);
        assertThat(res.cuitCuil()).isEqualTo("30-11223344-5");
        assertThat(res.razonSocial()).isEqualTo("Mi Empresa S.A.");
        verify(perfilRepository).saveAndFlush(any(Perfil.class));
    }

    @Test
    @DisplayName("solicitar perfil Monotributista con datos válidos crea solicitud Pendiente")
    void solicitarMonotributistaValido() {
        when(usuarioService.buscarPorId(10L)).thenReturn(Optional.of(usuario));
        when(perfilRepository.existsByUsuarioId(10L)).thenReturn(false);
        when(perfilRepository.existsByCuitCuil("20-33445566-7")).thenReturn(false);
        when(perfilRepository.existsByDniTitularAndTipo("33445566", TipoPerfil.MONOTRIBUTISTA)).thenReturn(false);
        when(fileStorageService.guardar(eq(constanciaPdf), any())).thenReturn("perfiles/10/constancia/uuid.pdf");
        when(perfilRepository.saveAndFlush(any(Perfil.class))).thenAnswer(inv -> {
            Perfil p = inv.getArgument(0);
            p.setId(2L);
            return p;
        });

        SolicitarPerfilRequest req = new SolicitarPerfilRequest(
            TipoPerfil.MONOTRIBUTISTA,
            null, null,
            "20-33445566-7",
            "Carlos",
            "Gómez",
            "33445566",
            null,
            constanciaPdf
        );

        PerfilResponse res = perfilService.solicitar(10L, req);

        assertThat(res.id()).isEqualTo(2L);
        assertThat(res.tipo()).isEqualTo(TipoPerfil.MONOTRIBUTISTA);
        assertThat(res.estado()).isEqualTo(EstadoPerfil.PENDIENTE);
        assertThat(res.nombre()).isEqualTo("Carlos");
        assertThat(res.apellido()).isEqualTo("Gómez");
        verify(perfilRepository).saveAndFlush(any(Perfil.class));
    }

    @Test
    @DisplayName("si la base rechaza la solicitud, se borra la constancia ya guardada")
    void fallaAlGuardarBorraConstancia() {
        when(usuarioService.buscarPorId(10L)).thenReturn(Optional.of(usuario));
        when(perfilRepository.existsByUsuarioId(10L)).thenReturn(false);
        when(perfilRepository.existsByCuitCuil("30-11223344-5")).thenReturn(false);
        when(fileStorageService.guardar(eq(constanciaPdf), any())).thenReturn("perfiles/10/constancia/uuid.pdf");
        when(perfilRepository.saveAndFlush(any(Perfil.class))).thenThrow(new RuntimeException("DB error"));

        SolicitarPerfilRequest req = new SolicitarPerfilRequest(
            TipoPerfil.EMPRESA,
            "30-11223344-5",
            "Mi Empresa S.A.",
            null, null, null, null,
            "1122334455",
            constanciaPdf
        );

        assertThatThrownBy(() -> perfilService.solicitar(10L, req))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("DB error");

        verify(fileStorageService).eliminar("perfiles/10/constancia/uuid.pdf");
    }

    @Test
    @DisplayName("solicitar con CUIT duplicado lanza BusinessException con 400 (CA-3)")
    void rechazaCuitDuplicado() {
        when(usuarioService.buscarPorId(10L)).thenReturn(Optional.of(usuario));
        when(perfilRepository.existsByUsuarioId(10L)).thenReturn(false);
        when(perfilRepository.existsByCuitCuil("30-11223344-5")).thenReturn(true);

        SolicitarPerfilRequest req = new SolicitarPerfilRequest(
            TipoPerfil.EMPRESA,
            "30-11223344-5",
            "Empresa S.A.",
            null, null, null, null, null,
            constanciaPdf
        );

        assertThatThrownBy(() -> perfilService.solicitar(10L, req))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Este CUIT ya se encuentra registrado");
    }

    @Test
    @DisplayName("solicitar Monotributista con DNI duplicado lanza BusinessException (CA-3)")
    void rechazaDniDuplicado() {
        when(usuarioService.buscarPorId(10L)).thenReturn(Optional.of(usuario));
        when(perfilRepository.existsByUsuarioId(10L)).thenReturn(false);
        when(perfilRepository.existsByCuitCuil("20-33445566-7")).thenReturn(false);
        when(perfilRepository.existsByDniTitularAndTipo("33445566", TipoPerfil.MONOTRIBUTISTA)).thenReturn(true);

        SolicitarPerfilRequest req = new SolicitarPerfilRequest(
            TipoPerfil.MONOTRIBUTISTA,
            null, null,
            "20-33445566-7",
            "Carlos",
            "Gómez",
            "33445566",
            null,
            constanciaPdf
        );

        assertThatThrownBy(() -> perfilService.solicitar(10L, req))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Este DNI ya está asociado a un Monotributista");
    }

    @Test
    @DisplayName("usuario que ya tiene solicitud pendiente no puede enviar otra (CA-6)")
    void rechazaSegundaSolicitud() {
        when(usuarioService.buscarPorId(10L)).thenReturn(Optional.of(usuario));
        when(perfilRepository.existsByUsuarioId(10L)).thenReturn(true);

        SolicitarPerfilRequest req = new SolicitarPerfilRequest(
            TipoPerfil.EMPRESA,
            "30-11223344-5",
            "Empresa S.A.",
            null, null, null, null, null,
            constanciaPdf
        );

        assertThatThrownBy(() -> perfilService.solicitar(10L, req))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Ya posees una solicitud de perfil registrada");
    }

    @Test
    @DisplayName("solicitar sin constancia o formato no PDF lanza BusinessException (CA-4)")
    void rechazaConstanciaInvalida() {
        when(usuarioService.buscarPorId(10L)).thenReturn(Optional.of(usuario));
        when(perfilRepository.existsByUsuarioId(10L)).thenReturn(false);

        MockMultipartFile noPdf = new MockMultipartFile(
            "constancia", "foto.png", "image/png", "bytes".getBytes()
        );

        SolicitarPerfilRequest req = new SolicitarPerfilRequest(
            TipoPerfil.EMPRESA,
            "30-11223344-5",
            "Empresa S.A.",
            null, null, null, null, null,
            noPdf
        );

        assertThatThrownBy(() -> perfilService.solicitar(10L, req))
            .isInstanceOf(BusinessException.class)
            .hasMessage("El archivo debe ser un PDF de hasta 8 MB");
    }

    // =========================================================================
    // US-18 TESTS
    // =========================================================================

    @Test
    @DisplayName("US-18: listar perfiles paginados devuelve PageResponse")
    void listarPerfilesPaginados() {
        Perfil p = new Perfil();
        p.setId(1L);
        p.setUsuario(usuario);
        p.setTipo(TipoPerfil.EMPRESA);
        p.setRazonSocial("Constructora Test");
        p.setCuitCuil("30-11223344-5");
        p.setEstado(EstadoPerfil.PENDIENTE);

        Pageable pageable = PageRequest.of(0, 10);
        when(perfilRepository.listarConFiltros(null, "test", pageable))
            .thenReturn(new PageImpl<>(List.of(p), pageable, 1));

        PageResponse<PerfilResponse> resultado = perfilService.listar(null, "test", pageable);

        assertThat(resultado.totalElements()).isEqualTo(1);
        assertThat(resultado.content()).hasSize(1);
        assertThat(resultado.content().get(0).razonSocial()).isEqualTo("Constructora Test");
    }

    @Test
    @DisplayName("US-18: obtener perfil por ID existente devuelve PerfilResponse")
    void obtenerPorIdExistente() {
        Perfil p = new Perfil();
        p.setId(5L);
        p.setUsuario(usuario);
        p.setTipo(TipoPerfil.MONOTRIBUTISTA);
        p.setNombre("Mario");
        p.setApellido("Bros");
        p.setCuitCuil("20-11223344-5");

        when(perfilRepository.findById(5L)).thenReturn(Optional.of(p));

        PerfilResponse res = perfilService.obtenerPorId(5L);

        assertThat(res.id()).isEqualTo(5L);
        assertThat(res.nombre()).isEqualTo("Mario");
        assertThat(res.email()).isEqualTo("usuario@test.com");
    }

    @Test
    @DisplayName("US-18: obtener perfil por ID inexistente lanza NotFoundException")
    void obtenerPorIdInexistente() {
        when(perfilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> perfilService.obtenerPorId(99L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("US-18: obtener constancia devuelve el recurso del storage")
    void obtenerConstanciaValida() {
        Perfil p = new Perfil();
        p.setId(1L);
        p.setConstanciaRuta("perfiles/1/constancia/file.pdf");

        Resource recursoMock = new ByteArrayResource("%PDF test".getBytes());
        when(perfilRepository.findById(1L)).thenReturn(Optional.of(p));
        when(fileStorageService.cargar("perfiles/1/constancia/file.pdf")).thenReturn(recursoMock);

        Resource res = perfilService.obtenerConstancia(1L);

        assertThat(res).isNotNull();
        assertThat(res).isSameAs(recursoMock);
    }

    // =========================================================================
    // US-20 TESTS
    // =========================================================================

    @Test
    @DisplayName("US-20: aprobar solicitud pendiente asigna rol EMPRESA y pasa a APROBADO")
    void aprobarSolicitudEmpresaExitosa() {
        Perfil p = new Perfil();
        p.setId(100L);
        p.setUsuario(usuario);
        p.setTipo(TipoPerfil.EMPRESA);
        p.setEstado(EstadoPerfil.PENDIENTE);
        p.setCuitCuil("30-99887766-5");

        Rol rolEmpresa = new Rol("EMPRESA", "Empresa");
        when(perfilRepository.findById(100L)).thenReturn(Optional.of(p));
        when(rolRepository.findByCodigo("EMPRESA")).thenReturn(Optional.of(rolEmpresa));
        when(perfilRepository.save(any(Perfil.class))).thenAnswer(inv -> inv.getArgument(0));

        PerfilResponse res = perfilService.aprobar(100L);

        assertThat(res.estado()).isEqualTo(EstadoPerfil.APROBADO);
        assertThat(usuario.getRoles()).contains(rolEmpresa);
        verify(usuarioRepository).save(usuario);
        verify(perfilRepository).save(p);
    }

    @Test
    @DisplayName("US-20: aprobar solicitud que no está pendiente lanza BusinessException")
    void aprobarSolicitudNoPendienteFalla() {
        Perfil p = new Perfil();
        p.setId(100L);
        p.setUsuario(usuario);
        p.setTipo(TipoPerfil.EMPRESA);
        p.setEstado(EstadoPerfil.APROBADO);

        when(perfilRepository.findById(100L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> perfilService.aprobar(100L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Solo se pueden aprobar solicitudes en estado pendiente");
    }

    @Test
    @DisplayName("US-20: rechazar solicitud pendiente con motivo pasa a RECHAZADO y guarda motivo")
    void rechazarSolicitudExitosa() {
        Perfil p = new Perfil();
        p.setId(101L);
        p.setUsuario(usuario);
        p.setTipo(TipoPerfil.MONOTRIBUTISTA);
        p.setEstado(EstadoPerfil.PENDIENTE);
        p.setCuitCuil("20-99887766-5");

        when(perfilRepository.findById(101L)).thenReturn(Optional.of(p));
        when(perfilRepository.save(any(Perfil.class))).thenAnswer(inv -> inv.getArgument(0));

        RechazarPerfilRequest req = new RechazarPerfilRequest("Constancia ilegible");
        PerfilResponse res = perfilService.rechazar(101L, req);

        assertThat(res.estado()).isEqualTo(EstadoPerfil.RECHAZADO);
        assertThat(res.motivoRechazo()).isEqualTo("Constancia ilegible");
        verify(perfilRepository).save(p);
    }

    @Test
    @DisplayName("US-20: rechazar solicitud sin motivo obligatorio lanza BusinessException")
    void rechazarSinMotivoFalla() {
        RechazarPerfilRequest req = new RechazarPerfilRequest("   ");

        assertThatThrownBy(() -> perfilService.rechazar(101L, req))
            .isInstanceOf(BusinessException.class)
            .hasMessage("El motivo de rechazo es obligatorio");
    }

    @Test
    @DisplayName("US-20: rechazar solicitud que no está pendiente lanza BusinessException")
    void rechazarSolicitudNoPendienteFalla() {
        Perfil p = new Perfil();
        p.setId(101L);
        p.setUsuario(usuario);
        p.setTipo(TipoPerfil.MONOTRIBUTISTA);
        p.setEstado(EstadoPerfil.RECHAZADO);

        when(perfilRepository.findById(101L)).thenReturn(Optional.of(p));

        RechazarPerfilRequest req = new RechazarPerfilRequest("Nuevo motivo");
        assertThatThrownBy(() -> perfilService.rechazar(101L, req))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Solo se pueden rechazar solicitudes en estado pendiente");
    }
}
