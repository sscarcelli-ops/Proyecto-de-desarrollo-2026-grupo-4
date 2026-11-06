package ar.edu.grupo4.accesos.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    private PasswordEncoder passwordEncoder;
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        usuarioService = new UsuarioService(usuarioRepository, rolRepository, passwordEncoder);
    }

    @Test
    void crearUsuarioHasheaPasswordYGuardaEmailLimpio() {
        when(usuarioRepository.existsByEmailIgnoreCase("nuevo@empresa.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        Usuario creado = usuarioService.crearUsuario("  nuevo@empresa.com  ", "Password123");

        assertThat(creado.getId()).isEqualTo(10L);
        assertThat(creado.getEmail()).isEqualTo("nuevo@empresa.com");
        assertThat(passwordEncoder.matches("Password123", creado.getPasswordHash())).isTrue();
    }

    @Test
    void crearUsuarioConEmailDuplicadoLanzaErrorNegocio() {
        when(usuarioRepository.existsByEmailIgnoreCase("existe@empresa.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crearUsuario("existe@empresa.com", "Password123"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Este email ya se encuentra registrado")
            .hasFieldOrPropertyWithValue("code", "EMAIL_DUPLICADO")
            .hasFieldOrPropertyWithValue("status", HttpStatus.UNPROCESSABLE_CONTENT);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("asignarRol agrega el rol al usuario y lo guarda")
    void asignarRolExitoso() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        Rol rol = org.mockito.Mockito.mock(Rol.class);

        when(usuarioRepository.findWithRolesById(10L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByCodigo("EMPRESA")).thenReturn(Optional.of(rol));

        usuarioService.asignarRol(10L, "EMPRESA");

        assertThat(usuario.getRoles()).contains(rol);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("asignarRol con rol inexistente lanza BusinessException")
    void asignarRolInexistenteFalla() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);

        when(usuarioRepository.findWithRolesById(10L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByCodigo("ROL_FANTASMA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.asignarRol(10L, "ROL_FANTASMA"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("No se encontró el rol ROL_FANTASMA");
    }

    @Test
    @DisplayName("asignarRol con usuario inexistente lanza NotFoundException")
    void asignarRolUsuarioInexistenteFalla() {
        when(usuarioRepository.findWithRolesById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.asignarRol(99L, "EMPRESA"))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("cerrarSesiones incrementa la versión de sesión")
    void cerrarSesionesIncrementaVersion() {
        usuarioService.cerrarSesiones(10L);

        verify(usuarioRepository).incrementarVersionSesion(10L);
    }
}
