package ar.edu.grupo4.accesos.usuario;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, passwordEncoder);
    }

    @Test
    void crearUsuarioGuardaConHashYEmailLimpio() {
        when(usuarioRepository.existsByEmailIgnoreCase("nuevo@empresa.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
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
            .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verify(usuarioRepository, never()).save(any());
    }
}
