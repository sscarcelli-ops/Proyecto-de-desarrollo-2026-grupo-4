package ar.edu.grupo4.accesos.garita;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.garita.dto.BusquedaDniResponse;
import ar.edu.grupo4.accesos.garita.dto.IngresoActivoInfo;
import ar.edu.grupo4.accesos.garita.dto.MovimientoResponse;
import ar.edu.grupo4.accesos.garita.dto.VerificacionResponse;
import ar.edu.grupo4.accesos.garita.dto.VinculoResultado;
import ar.edu.grupo4.accesos.usuario.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GaritaServiceTest {

    private GaritaRepository repository;
    private GaritaService service;
    private final UsuarioActual usuarioActual = mock(UsuarioActual.class);

    @BeforeEach
    void setUp() {
        repository = mock(GaritaRepository.class);
        service = new GaritaService(repository, usuarioActual);
        // El Guardia del token trabaja en el barrio 1 y su usuario es el 99
        when(usuarioActual.barrioId()).thenReturn(1L);
        when(usuarioActual.id()).thenReturn(99L);
    }

    // ── US-37: Buscar por DNI ────────────────────────────────────────────────────────

    @Test
    @DisplayName("US-37: Si el DNI no existe en ningún vínculo, lanza BusinessException")
    void buscarPorDni_cuandoNoExiste_lanzaBusinessException() {
        when(repository.buscarVinculosPorDni("99999999")).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.buscarPorDni("99999999"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No se encontró un trabajador con ese DNI");
    }

    @Test
    @DisplayName("US-37: Si el DNI existe, retorna datos del trabajador y lista de vínculos")
    void buscarPorDni_cuandoExiste_retornaDatosYVinculos() {
        String dni = "30111222";
        VinculoResultado vinculo1 = new VinculoResultado(10L, 1L, "Limpiezas S.A.", "EMPRESA");
        VinculoResultado vinculo2 = new VinculoResultado(20L, 2L, "Monotributista", "MONOTRIBUTISTA");
        GaritaRepository.TrabajadorResumen resumen = new GaritaRepository.TrabajadorResumen("Juan", "Pérez", "fotos/foto.jpg");

        when(repository.buscarVinculosPorDni(dni)).thenReturn(List.of(vinculo1, vinculo2));
        when(repository.buscarResumenPorDni(dni)).thenReturn(Optional.of(resumen));
        when(repository.buscarIngresoActivo(dni, 1L)).thenReturn(Optional.empty());

        BusquedaDniResponse response = service.buscarPorDni(dni);

        assertThat(response.nombre()).isEqualTo("Juan");
        assertThat(response.apellido()).isEqualTo("Pérez");
        assertThat(response.dni()).isEqualTo(dni);
        assertThat(response.tieneFoto()).isTrue();
        assertThat(response.vinculosBusqueda()).hasSize(2);
        assertThat(response.ingresoActivo()).isNull();
    }

    @Test
    @DisplayName("US-37: Detecta ingreso activo si el trabajador está dentro del barrio")
    void buscarPorDni_cuandoTieneIngresoActivo_retornaInfo() {
        String dni = "30111222";
        VinculoResultado vinculo = new VinculoResultado(10L, 1L, "Limpiezas S.A.", "EMPRESA");
        GaritaRepository.TrabajadorResumen resumen = new GaritaRepository.TrabajadorResumen("Juan", "Pérez", null);
        IngresoActivoInfo ingreso = new IngresoActivoInfo(100L, 10L, "Limpiezas S.A.", "EMPRESA");

        when(repository.buscarVinculosPorDni(dni)).thenReturn(List.of(vinculo));
        when(repository.buscarResumenPorDni(dni)).thenReturn(Optional.of(resumen));
        when(repository.buscarIngresoActivo(dni, 1L)).thenReturn(Optional.of(ingreso));

        BusquedaDniResponse response = service.buscarPorDni(dni);

        assertThat(response.tieneFoto()).isFalse();
        assertThat(response.ingresoActivo()).isNotNull();
        assertThat(response.ingresoActivo().nombreVinculo()).isEqualTo("Limpiezas S.A.");
    }

    // ── US-38: Verificar acceso ──────────────────────────────────────────────────────

    @Test
    @DisplayName("US-38: Delega la verificación en la función PG a través del repository")
    void verificarAcceso_delegaEnRepository() {
        VerificacionResponse mockResp = new VerificacionResponse(true, "");
        when(repository.verificarAcceso(10L, 1L)).thenReturn(mockResp);

        VerificacionResponse response = service.verificarAcceso(10L);

        assertThat(response.habilitado()).isTrue();
        verify(repository).verificarAcceso(10L, 1L);
    }

    // ── US-39: Registrar ingreso ─────────────────────────────────────────────────────

    @Test
    @DisplayName("US-39: Registra ingreso cuando la verificación resulta habilitada")
    void registrarIngreso_cuandoHabilitado_registraMovimiento() {
        when(repository.verificarAcceso(10L, 1L)).thenReturn(new VerificacionResponse(true, ""));
        MovimientoResponse mockMov = new MovimientoResponse(1L, "INGRESO", OffsetDateTime.now(), "Juan Pérez", "Limpiezas S.A.");
        when(repository.registrarIngreso(10L, 1L, 99L)).thenReturn(mockMov);

        MovimientoResponse response = service.registrarIngreso(10L);

        assertThat(response.tipo()).isEqualTo("INGRESO");
        verify(repository).registrarIngreso(10L, 1L, 99L);
    }

    @Test
    @DisplayName("US-39: Si está deshabilitado al registrar ingreso, rechaza con BusinessException")
    void registrarIngreso_cuandoDeshabilitado_lanzaBusinessException() {
        when(repository.verificarAcceso(10L, 1L))
                .thenReturn(new VerificacionResponse(false, "documentación vencida (ART)"));

        assertThatThrownBy(() -> service.registrarIngreso(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("documentación vencida (ART)");

        verify(repository, never()).registrarIngreso(anyLong(), anyLong(), anyLong());
    }

    // ── US-40: Registrar egreso ──────────────────────────────────────────────────────

    @Test
    @DisplayName("US-40: Registra egreso si el trabajador cuenta con un ingreso activo")
    void registrarEgreso_cuandoTieneIngresoActivo_registraMovimiento() {
        when(repository.buscarIngresoActivoPorTrabajador(10L, 1L)).thenReturn(Optional.of(100L));
        MovimientoResponse mockMov = new MovimientoResponse(2L, "EGRESO", OffsetDateTime.now(), "Juan Pérez", "Limpiezas S.A.");
        when(repository.registrarEgreso(10L, 1L, 99L)).thenReturn(mockMov);

        MovimientoResponse response = service.registrarEgreso(10L);

        assertThat(response.tipo()).isEqualTo("EGRESO");
        verify(repository).registrarEgreso(10L, 1L, 99L);
    }

    @Test
    @DisplayName("US-40: Si no tiene ingreso activo, no permite egreso y lanza BusinessException")
    void registrarEgreso_cuandoNoTieneIngresoActivo_lanzaBusinessException() {
        when(repository.buscarIngresoActivoPorTrabajador(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarEgreso(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("El trabajador no tiene un ingreso activo en este barrio");

        verify(repository, never()).registrarEgreso(anyLong(), anyLong(), anyLong());
    }
}
