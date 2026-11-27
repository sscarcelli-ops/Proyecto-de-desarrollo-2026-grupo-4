package ar.edu.grupo4.accesos.movimiento;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.movimiento.dto.EntidadFiltroResponse;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoFiltros;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoHistorialResponse;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
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
 * US-36: Permisos por rol en MovimientoController (@PreAuthorize("hasRole('OPERADOR')")).
 * Contexto mínimo de Spring con la seguridad de métodos activa y sin base de datos (el Service y UsuarioActual son mocks),
 * igual que en GuardiaControllerPermisosTest (US-32) y OperadorControllerPermisosTest (US-15).
 */
class MovimientoControllerPermisosTest {

    @Configuration
    @EnableMethodSecurity
    static class Contexto {
        @Bean
        MovimientoService movimientoService() {
            return mock(MovimientoService.class);
        }

        @Bean
        UsuarioActual usuarioActual() {
            return mock(UsuarioActual.class);
        }

        @Bean
        MovimientoController movimientoController(MovimientoService movimientoService, UsuarioActual usuarioActual) {
            return new MovimientoController(movimientoService, usuarioActual);
        }
    }

    private AnnotationConfigApplicationContext contexto;
    private MovimientoController controller;
    private MovimientoService service;
    private UsuarioActual usuarioActual;

    @BeforeEach
    void setUp() {
        contexto = new AnnotationConfigApplicationContext(Contexto.class);
        controller = contexto.getBean(MovimientoController.class);
        service = contexto.getBean(MovimientoService.class);
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
    @DisplayName("US-36: el Operador puede consultar el historial de su barrio")
    void operadorPuedeConsultarHistorial() {
        sesionCon("OPERADOR");
        when(usuarioActual.barrioId()).thenReturn(1L);

        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<MovimientoHistorialResponse> respuesta = new PageResponse<>(List.of(), 0, 10, 0, 0);
        when(service.consultarHistorial(eq(1L), any(MovimientoFiltros.class), eq(pageable))).thenReturn(respuesta);

        assertThat(controller.consultarHistorial(null, null, null, null, pageable)).isEqualTo(respuesta);
    }

    @Test
    @DisplayName("US-36: el Operador puede listar las entidades para el filtro")
    void operadorPuedeListarEntidades() {
        sesionCon("OPERADOR");
        when(usuarioActual.barrioId()).thenReturn(1L);

        List<EntidadFiltroResponse> entidades = List.of(new EntidadFiltroResponse(10L, "Limpiezas S.A.", "EMPRESA"));
        when(service.listarEntidadesFiltro(1L)).thenReturn(entidades);

        assertThat(controller.listarEntidadesFiltro()).isEqualTo(entidades);
    }

    @Test
    @DisplayName("US-36: el Operador sin barrio recibe 403 SIN_BARRIO")
    void operadorSinBarrioRecibe403() {
        sesionCon("OPERADOR");
        when(usuarioActual.barrioId())
            .thenThrow(new BusinessException("SIN_BARRIO", "No tenés un barrio asignado.", HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> controller.consultarHistorial(null, null, null, null, PageRequest.of(0, 10)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", "SIN_BARRIO")
            .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        assertThatThrownBy(() -> controller.listarEntidadesFiltro())
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", "SIN_BARRIO")
            .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("US-36: sin autenticación se lanza error de autenticación")
    void sinTokenDaErrorAutenticacion() {
        // Sin Authentication en SecurityContextHolder
        assertThatThrownBy(() -> controller.consultarHistorial(null, null, null, null, PageRequest.of(0, 10)))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        assertThatThrownBy(() -> controller.listarEntidadesFiltro())
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        verifyNoInteractions(service);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"ADMIN", "GUARDIA", "EMPRESA", "MONOTRIBUTISTA"})
    @DisplayName("US-36: otro rol recibe 403 al consultar historial o listar entidades, y no llega al Service ni a UsuarioActual")
    void otroRolRecibe403(String rol) {
        sesionCon(rol);

        assertThatThrownBy(() -> controller.consultarHistorial(null, null, null, null, PageRequest.of(0, 10)))
            .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> controller.listarEntidadesFiltro())
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(service);
        verifyNoInteractions(usuarioActual);
    }

    @Test
    @DisplayName("US-36: no se puede ordenar el historial por ?sort= (se rechaza con ORDEN_INVALIDO)")
    void sortSeRechaza() {
        sesionCon("OPERADOR");
        when(usuarioActual.barrioId()).thenReturn(1L);

        Pageable conSort = PageRequest.of(0, 10, Sort.by("trabajadorDni"));

        assertThatThrownBy(() -> controller.consultarHistorial(null, null, null, null, conSort))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", "ORDEN_INVALIDO");

        verifyNoInteractions(service);
    }
}
