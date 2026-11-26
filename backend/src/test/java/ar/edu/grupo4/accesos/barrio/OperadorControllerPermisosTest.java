package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.OperadorListadoResponse;
import ar.edu.grupo4.accesos.common.paging.PageResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import ar.edu.grupo4.accesos.common.exception.BusinessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * US-15: solo el Administrador lista los Operadores (@PreAuthorize("hasRole('ADMIN')") en OperadorController).
 * Contexto mínimo de Spring con la seguridad de métodos activa y sin base de datos (el Service es un mock), igual
 * que el de los Guardias (US-32).
 */
class OperadorControllerPermisosTest {

    @Configuration
    @EnableMethodSecurity
    static class Contexto {
        @Bean
        OperadorService operadorService() {
            return mock(OperadorService.class);
        }

        @Bean
        OperadorController operadorController(OperadorService operadorService) {
            return new OperadorController(operadorService);
        }
    }

    private AnnotationConfigApplicationContext contexto;
    private OperadorController controller;
    private OperadorService service;

    @BeforeEach
    void setUp() {
        contexto = new AnnotationConfigApplicationContext(Contexto.class);
        controller = contexto.getBean(OperadorController.class);
        service = contexto.getBean(OperadorService.class);
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
    @DisplayName("US-15: el Administrador puede listar los Operadores")
    void adminPuedeListar() {
        sesionCon("ADMIN");
        var pageable = PageRequest.of(0, 10);
        var pagina = new PageResponse<OperadorListadoResponse>(List.of(), 0, 10, 0, 0);
        when(service.listar(null, null, pageable)).thenReturn(pagina);

        assertThat(controller.listar(null, null, pageable)).isEqualTo(pagina);
    }

    @Test
    @DisplayName("US-15: no se puede ordenar el listado por un campo cualquiera (?sort=)")
    void ordenPorCampoCualquieraSeRechaza() {
        sesionCon("ADMIN");

        assertThatThrownBy(() -> controller.listar(null, null, PageRequest.of(0, 10, Sort.by("passwordHash"))))
            .isInstanceOf(BusinessException.class);
        verifyNoInteractions(service);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"OPERADOR", "GUARDIA", "EMPRESA", "MONOTRIBUTISTA"})
    @DisplayName("US-15 y US-16: otro rol recibe 403 al listar, crear, ver o modificar un Operador, y no llega al Service")
    void otroRolRecibe403(String rol) {
        sesionCon(rol);

        assertThatThrownBy(() -> controller.listar(null, null, PageRequest.of(0, 10))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.crear(null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.disponibles()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.obtener(1L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.modificar(1L, null)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(service);
    }
}
