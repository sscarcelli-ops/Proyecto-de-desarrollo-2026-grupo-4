package ar.edu.grupo4.accesos.perfil;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.perfil.dto.PerfilBarrioResponse;
import ar.edu.grupo4.accesos.usuario.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** US-33 y US-34: listado de perfiles aprobados y habilitación en el barrio del Operador. */
class HabilitacionBarrioServiceTest {

    private static final Long BARRIO_ID = 1L;

    private final PerfilRepository perfilRepository = mock(PerfilRepository.class);
    private final UsuarioActual usuarioActual = mock(UsuarioActual.class);
    private HabilitacionBarrioService service;

    @BeforeEach
    void setUp() {
        service = new HabilitacionBarrioService(perfilRepository, usuarioActual);
        // El Operador de la sesión tiene a cargo el barrio 1
        when(usuarioActual.barrioId()).thenReturn(BARRIO_ID);
    }

    private Perfil empresa(Long id, String razonSocial) {
        Perfil p = new Perfil();
        p.setId(id);
        p.setTipo(TipoPerfil.EMPRESA);
        p.setEstado(EstadoPerfil.APROBADO);
        p.setCuitCuil("30-11223344-5");
        p.setRazonSocial(razonSocial);
        return p;
    }

    private Perfil monotributista(Long id, String nombre, String apellido) {
        Perfil p = new Perfil();
        p.setId(id);
        p.setTipo(TipoPerfil.MONOTRIBUTISTA);
        p.setEstado(EstadoPerfil.APROBADO);
        p.setCuitCuil("20-30111222-3");
        p.setNombre(nombre);
        p.setApellido(apellido);
        return p;
    }

    // ── US-33: Listar Empresas y Monotributistas aprobados ───────────────────────────

    @Test
    @DisplayName("US-33 CA-1: cada perfil aprobado indica si está habilitado en el barrio del Operador")
    void listar_marcaLaHabilitacionDelBarrioDeLaSesion() {
        Pageable pageable = PageRequest.of(0, 10);
        Perfil limpiezas = empresa(10L, "Limpiezas del Norte S.R.L.");
        Perfil marcos = monotributista(20L, "Marcos", "Aprobado");
        when(perfilRepository.listarConFiltros(EstadoPerfil.APROBADO, null, pageable))
            .thenReturn(new PageImpl<>(List.of(limpiezas, marcos), pageable, 2));
        when(perfilRepository.perfilesHabilitadosEnBarrio(BARRIO_ID, List.of(10L, 20L))).thenReturn(List.of(10L));

        PageResponse<PerfilBarrioResponse> resultado = service.listar(pageable);

        assertThat(resultado.content()).extracting(PerfilBarrioResponse::nombre, PerfilBarrioResponse::habilitado)
            .containsExactly(tuple("Limpiezas del Norte S.R.L.", true), tuple("Marcos Aprobado", false));
        assertThat(resultado.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("US-33 CA-1 y CA-2: se consultan solo los perfiles Aprobados, en el orden alfabético de la consulta")
    void listar_consultaSoloPerfilesAprobados() {
        Pageable pageable = PageRequest.of(0, 10);
        when(perfilRepository.listarConFiltros(any(), any(), any())).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.listar(pageable);

        verify(perfilRepository).listarConFiltros(EstadoPerfil.APROBADO, null, pageable);
    }

    @Test
    @DisplayName("US-33 CA-4: sin perfiles aprobados devuelve una página vacía sin consultar habilitaciones")
    void listar_sinPerfilesAprobados_devuelvePaginaVacia() {
        Pageable pageable = PageRequest.of(0, 10);
        when(perfilRepository.listarConFiltros(EstadoPerfil.APROBADO, null, pageable))
            .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<PerfilBarrioResponse> resultado = service.listar(pageable);

        assertThat(resultado.content()).isEmpty();
        assertThat(resultado.totalElements()).isZero();
        verify(perfilRepository, never()).perfilesHabilitadosEnBarrio(anyLong(), anyCollection());
    }

    @Test
    @DisplayName("US-42: sin barrio en la sesión no se lista nada")
    void listar_sinBarrio_rechazaSinConsultar() {
        when(usuarioActual.barrioId())
            .thenThrow(new BusinessException("SIN_BARRIO", "No tenés un barrio asignado.", HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> service.listar(PageRequest.of(0, 10))).isInstanceOf(BusinessException.class);
        verify(perfilRepository, never()).listarConFiltros(any(), any(), any());
    }

    // ── US-34: Habilitar o deshabilitar un perfil en el barrio ───────────────────────

    @Test
    @DisplayName("US-34 CA-1: habilitar agrega la habilitación solo en el barrio del Operador")
    void habilitar_perfilAprobado_loHabilitaEnElBarrioDeLaSesion() {
        when(perfilRepository.findById(10L)).thenReturn(Optional.of(empresa(10L, "Limpiezas del Norte S.R.L.")));

        PerfilBarrioResponse resultado = service.habilitar(10L);

        assertThat(resultado.habilitado()).isTrue();
        verify(perfilRepository).habilitarEnBarrio(10L, BARRIO_ID);
    }

    @Test
    @DisplayName("US-34 CA-2: deshabilitar quita la habilitación solo en el barrio del Operador")
    void deshabilitar_perfilAprobado_loDeshabilitaEnElBarrioDeLaSesion() {
        when(perfilRepository.findById(10L)).thenReturn(Optional.of(empresa(10L, "Limpiezas del Norte S.R.L.")));

        PerfilBarrioResponse resultado = service.deshabilitar(10L);

        assertThat(resultado.habilitado()).isFalse();
        verify(perfilRepository).deshabilitarEnBarrio(10L, BARRIO_ID);
    }

    @Test
    @DisplayName("US-34 CA-3: habilitar o deshabilitar no cambia el estado global del perfil")
    void habilitarYDeshabilitar_noCambianElEstadoGlobal() {
        Perfil perfil = empresa(10L, "Limpiezas del Norte S.R.L.");
        when(perfilRepository.findById(10L)).thenReturn(Optional.of(perfil));

        service.habilitar(10L);
        service.deshabilitar(10L);

        assertThat(perfil.getEstado()).isEqualTo(EstadoPerfil.APROBADO);
        verify(perfilRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-34: un perfil que no está Aprobado responde como inexistente y no se toca la habilitación")
    void habilitar_perfilNoAprobado_respondeNoEncontrado() {
        Perfil suspendido = empresa(10L, "Limpiezas del Norte S.R.L.");
        suspendido.setEstado(EstadoPerfil.SUSPENDIDO);
        when(perfilRepository.findById(10L)).thenReturn(Optional.of(suspendido));
        when(perfilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.habilitar(10L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.deshabilitar(10L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.habilitar(99L)).isInstanceOf(NotFoundException.class);
        verify(perfilRepository, never()).habilitarEnBarrio(anyLong(), anyLong());
        verify(perfilRepository, never()).deshabilitarEnBarrio(anyLong(), anyLong());
    }
}
