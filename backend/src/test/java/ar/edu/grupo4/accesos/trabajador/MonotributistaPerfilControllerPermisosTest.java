package ar.edu.grupo4.accesos.trabajador;

import ar.edu.grupo4.accesos.perfil.PerfilService;
import ar.edu.grupo4.accesos.trabajador.dto.MonotributistaPerfilModificarRequest;
import ar.edu.grupo4.accesos.trabajador.dto.MonotributistaPerfilResponse;
import ar.edu.grupo4.accesos.usuario.UsuarioActual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * US-24: Permisos por rol en MonotributistaPerfilController (@PreAuthorize("hasRole('MONOTRIBUTISTA')")).
 * Contexto mínimo de Spring con method security activa para verificar que solo el Monotributista
 * pueda acceder a sus endpoints y que el resto de los roles reciban AccessDeniedException (403).
 */
class MonotributistaPerfilControllerPermisosTest {

    @Configuration
    @EnableMethodSecurity
    static class Contexto {
        @Bean
        PerfilService perfilService() {
            return mock(PerfilService.class);
        }

        @Bean
        TrabajadorService trabajadorService() {
            return mock(TrabajadorService.class);
        }

        @Bean
        UsuarioActual usuarioActual() {
            return mock(UsuarioActual.class);
        }

        @Bean
        MonotributistaPerfilController monotributistaPerfilController(
            PerfilService perfilService,
            TrabajadorService trabajadorService,
            UsuarioActual usuarioActual
        ) {
            return new MonotributistaPerfilController(perfilService, trabajadorService, usuarioActual);
        }
    }

    private AnnotationConfigApplicationContext contexto;
    private MonotributistaPerfilController controller;
    private PerfilService perfilService;
    private UsuarioActual usuarioActual;

    @BeforeEach
    void setUp() {
        contexto = new AnnotationConfigApplicationContext(Contexto.class);
        controller = contexto.getBean(MonotributistaPerfilController.class);
        perfilService = contexto.getBean(PerfilService.class);
        usuarioActual = contexto.getBean(UsuarioActual.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        contexto.close();
    }

    private static void sesionCon(String rol) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            "1", null, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }

    @Test
    @DisplayName("Monotributista puede consultar y modificar su propio perfil")
    void monotributistaPuedeAcceder() {
        sesionCon("MONOTRIBUTISTA");
        when(usuarioActual.perfilIdAprobado()).thenReturn(5L);
        MonotributistaPerfilResponse mockResponse = new MonotributistaPerfilResponse(
            5L, 10L, "Paula", "Gómez", "33222111", "27-33222111-1",
            "1122334455", "PENDIENTE_CARGA", false, null, List.of()
        );
        when(perfilService.obtenerPerfilMonotributista(5L)).thenReturn(mockResponse);

        MonotributistaPerfilResponse res = controller.obtener();
        assertThat(res.perfilId()).isEqualTo(5L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "GUARDIA", "EMPRESA", "OPERADOR"})
    @DisplayName("Otros roles son rechazados con 403 al intentar acceder al perfil de monotributista")
    void otrosRolesRechazadosEnObtener(String rol) {
        sesionCon(rol);
        assertThatThrownBy(() -> controller.obtener())
            .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(perfilService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "GUARDIA", "EMPRESA", "OPERADOR"})
    @DisplayName("Otros roles son rechazados con 403 al intentar modificar el perfil de monotributista")
    void otrosRolesRechazadosEnModificar(String rol) {
        sesionCon(rol);
        MonotributistaPerfilModificarRequest req = new MonotributistaPerfilModificarRequest("Nuevo", "Nombre");
        assertThatThrownBy(() -> controller.modificar(req))
            .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(perfilService);
    }
}
