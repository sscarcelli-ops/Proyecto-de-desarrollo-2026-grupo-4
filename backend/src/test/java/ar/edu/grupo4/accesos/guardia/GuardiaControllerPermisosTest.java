package ar.edu.grupo4.accesos.guardia;

import ar.edu.grupo4.accesos.guardia.dto.GuardiaResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * US-32: solo el Operador gestiona Guardias (@PreAuthorize("hasRole('OPERADOR')") en GuardiaController).
 * Los tests con MockMvc standalone no pasan por Spring Security, así que acá se arma un contexto mínimo de Spring
 * con la seguridad de métodos activa: el Controller real queda envuelto por el proxy que evalúa @PreAuthorize.
 * Sin base de datos: el Service es un mock.
 */
class GuardiaControllerPermisosTest {

    @Configuration
    @EnableMethodSecurity
    static class Contexto {
        @Bean
        GuardiaService guardiaService() {
            return mock(GuardiaService.class);
        }

        @Bean
        GuardiaController guardiaController(GuardiaService guardiaService) {
            return new GuardiaController(guardiaService);
        }
    }

    private AnnotationConfigApplicationContext contexto;
    private GuardiaController controller;
    private GuardiaService service;

    @BeforeEach
    void setUp() {
        contexto = new AnnotationConfigApplicationContext(Contexto.class);
        controller = contexto.getBean(GuardiaController.class);
        service = contexto.getBean(GuardiaService.class);
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
    @DisplayName("US-32: el Operador puede listar los Guardias de su barrio")
    void operadorPuedeListar() {
        sesionCon("OPERADOR");
        List<GuardiaResponse> guardias = List.of(new GuardiaResponse(4L, "Gustavo", "Guardia", "guardia@seed.test", true));
        when(service.listar()).thenReturn(guardias);

        assertThat(controller.listar()).isEqualTo(guardias);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"ADMIN", "GUARDIA", "EMPRESA", "MONOTRIBUTISTA"})
    @DisplayName("US-32: otro rol recibe 403 al crear, listar, ver, modificar o cambiar el estado, y no llega al Service")
    void otroRolRecibe403(String rol) {
        sesionCon(rol);

        // La seguridad se evalúa antes de ejecutar el método: los argumentos no llegan a usarse
        assertThatThrownBy(() -> controller.crear(null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.listar()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.obtener(4L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.modificar(4L, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.cambiarEstado(4L, null)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(service);
    }
}
