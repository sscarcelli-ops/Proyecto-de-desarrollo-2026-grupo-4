package ar.edu.grupo4.accesos.perfil;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.storage.FileStorageService;
import ar.edu.grupo4.accesos.perfil.dto.PerfilResponse;
import ar.edu.grupo4.accesos.perfil.dto.SolicitarPerfilRequest;
import ar.edu.grupo4.accesos.usuario.Usuario;
import ar.edu.grupo4.accesos.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

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
    private UsuarioRepository usuarioRepository;

    @Mock
    private FileStorageService fileStorageService;

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
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(perfilRepository.existsByUsuarioId(10L)).thenReturn(false);
        when(perfilRepository.existsByCuitCuil("30-11223344-5")).thenReturn(false);
        when(fileStorageService.guardar(eq(constanciaPdf), any())).thenReturn("perfiles/10/constancia/uuid.pdf");
        when(perfilRepository.save(any(Perfil.class))).thenAnswer(inv -> {
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
        assertThat(res.constanciaRuta()).isEqualTo("perfiles/10/constancia/uuid.pdf");
        verify(perfilRepository).save(any(Perfil.class));
    }

    @Test
    @DisplayName("solicitar perfil Monotributista con datos válidos crea solicitud Pendiente")
    void solicitarMonotributistaValido() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(perfilRepository.existsByUsuarioId(10L)).thenReturn(false);
        when(perfilRepository.existsByCuitCuil("20-33445566-7")).thenReturn(false);
        when(perfilRepository.existsByDniTitularAndTipo("33445566", TipoPerfil.MONOTRIBUTISTA)).thenReturn(false);
        when(fileStorageService.guardar(eq(constanciaPdf), any())).thenReturn("perfiles/10/constancia/uuid.pdf");
        when(perfilRepository.save(any(Perfil.class))).thenAnswer(inv -> {
            Perfil p = inv.getArgument(0);
            p.setId(2L);
            return p;
        });

        SolicitarPerfilRequest req = new SolicitarPerfilRequest(
            TipoPerfil.MONOTRIBUTISTA,
            null, null,
            "20-33445566-7",
            "Juan",
            "Pérez",
            "33445566",
            null,
            constanciaPdf
        );

        PerfilResponse res = perfilService.solicitar(10L, req);

        assertThat(res.id()).isEqualTo(2L);
        assertThat(res.tipo()).isEqualTo(TipoPerfil.MONOTRIBUTISTA);
        assertThat(res.estado()).isEqualTo(EstadoPerfil.PENDIENTE);
        assertThat(res.nombre()).isEqualTo("Juan");
        assertThat(res.apellido()).isEqualTo("Pérez");
        assertThat(res.dniTitular()).isEqualTo("33445566");
    }

    @Test
    @DisplayName("solicitar con DNI duplicado de Monotributista lanza BusinessException (CA-3)")
    void rechazaDniDuplicadoMonotributista() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(perfilRepository.existsByUsuarioId(10L)).thenReturn(false);
        when(perfilRepository.existsByCuitCuil("20-33445566-7")).thenReturn(false);
        when(perfilRepository.existsByDniTitularAndTipo("33445566", TipoPerfil.MONOTRIBUTISTA)).thenReturn(true);

        SolicitarPerfilRequest req = new SolicitarPerfilRequest(
            TipoPerfil.MONOTRIBUTISTA,
            null, null,
            "20-33445566-7",
            "Juan",
            "Pérez",
            "33445566",
            null,
            constanciaPdf
        );

        assertThatThrownBy(() -> perfilService.solicitar(10L, req))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Este DNI ya está asociado a un Monotributista");
    }

    @Test
    @DisplayName("solicitar cuando ya tiene perfil registrado lanza SOLICITUD_DUPLICADA (CA-6)")
    void rechazaSegundaSolicitud() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
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
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
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
}
