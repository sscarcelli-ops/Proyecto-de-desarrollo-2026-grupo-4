package ar.edu.grupo4.accesos.trabajador;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorEditarRequest;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorRequest;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TrabajadorServiceTest {

    private final TrabajadorRepository trabajadorRepository = mock(TrabajadorRepository.class);
    private final PerfilIdResolver perfilIdResolver = mock(PerfilIdResolver.class);
    private TrabajadorService service;

    private static final Long USUARIO_ID = 10L;
    private static final Long PERFIL_ID = 100L;
    private static final Long OTRO_PERFIL_ID = 200L;

    @BeforeEach
    void setUp() {
        service = new TrabajadorService(trabajadorRepository, perfilIdResolver);
        when(perfilIdResolver.resolver(USUARIO_ID)).thenReturn(PERFIL_ID);
    }

    @Test
    @DisplayName("US-25: Registrar trabajador exitosamente con estado PENDIENTE_CARGA")
    void crearTrabajadorExitoso() {
        TrabajadorRequest request = new TrabajadorRequest("Lucas", "Martínez", "35123456");
        when(trabajadorRepository.existsByPerfilIdAndDniIgnoreCase(PERFIL_ID, "35123456")).thenReturn(false);

        Trabajador guardado = new Trabajador();
        guardado.setId(1L);
        guardado.setPerfilId(PERFIL_ID);
        guardado.setNombre("Lucas");
        guardado.setApellido("Martínez");
        guardado.setDni("35123456");
        guardado.setDocumentos(new ArrayList<>());
        when(trabajadorRepository.save(any(Trabajador.class))).thenReturn(guardado);

        TrabajadorResponse response = service.crear(USUARIO_ID, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nombre()).isEqualTo("Lucas");
        assertThat(response.apellido()).isEqualTo("Martínez");
        assertThat(response.dni()).isEqualTo("35123456");
        assertThat(response.estadoDocumental()).isEqualTo("PENDIENTE_CARGA");
        verify(trabajadorRepository).save(any(Trabajador.class));
    }

    @Test
    @DisplayName("US-25 / RN-11: Rechaza registro si el DNI ya existe en la misma empresa (TRABAJADOR_DUPLICADO)")
    void crearTrabajadorDniDuplicadoEnMismaEmpresaLanzaError() {
        TrabajadorRequest request = new TrabajadorRequest("Ana", "Vigente", "30777888");
        when(trabajadorRepository.existsByPerfilIdAndDniIgnoreCase(PERFIL_ID, "30777888")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(USUARIO_ID, request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Este trabajador ya está registrado en tu empresa");

        verify(trabajadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-26 / RF-23: Aislamiento entre empresas. Si se pide un trabajador ajeno, devuelve 404")
    void obtenerTrabajadorDeOtraEmpresaDevuelve404() {
        Long trabajadorAjenoId = 99L;
        when(trabajadorRepository.findByIdAndPerfilId(trabajadorAjenoId, PERFIL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(USUARIO_ID, trabajadorAjenoId))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("US-26: Modificar nombre y apellido exitosamente")
    void editarTrabajadorModificaNombreYApellido() {
        Long trabajadorId = 5L;
        Trabajador existente = new Trabajador();
        existente.setId(trabajadorId);
        existente.setPerfilId(PERFIL_ID);
        existente.setNombre("Carlos");
        existente.setApellido("Vencido");
        existente.setDni("28999000");
        existente.setDocumentos(new ArrayList<>());

        when(trabajadorRepository.findByIdAndPerfilId(trabajadorId, PERFIL_ID)).thenReturn(Optional.of(existente));

        TrabajadorEditarRequest request = new TrabajadorEditarRequest("Carlos Alberto", "Vencido Gómez");
        TrabajadorResponse response = service.editar(USUARIO_ID, trabajadorId, request);

        assertThat(response.nombre()).isEqualTo("Carlos Alberto");
        assertThat(response.apellido()).isEqualTo("Vencido Gómez");
        assertThat(response.dni()).isEqualTo("28999000");
    }

    @Test
    @DisplayName("RN-04: Cálculo de estado documental Vigente vs Vencido")
    void calculoEstadoDocumental() {
        Trabajador t = new Trabajador();
        t.setId(1L);
        t.setPerfilId(PERFIL_ID);
        t.setNombre("Prueba");
        t.setApellido("Doc");
        t.setDni("31000111");

        Documento docVigente = new Documento();
        docVigente.setTipo(TipoDocumento.ART);
        docVigente.setFechaVencimiento(LocalDate.now().plusMonths(3));

        Documento docVencido = new Documento();
        docVencido.setTipo(TipoDocumento.SEGURO_VIDA);
        docVencido.setFechaVencimiento(LocalDate.now().minusDays(1));

        t.setDocumentos(new ArrayList<>());
        assertThat(service.calcularEstadoDocumental(t)).isEqualTo("PENDIENTE_CARGA");

        t.getDocumentos().add(docVigente);
        assertThat(service.calcularEstadoDocumental(t)).isEqualTo("VIGENTE");

        t.getDocumentos().add(docVencido);
        assertThat(service.calcularEstadoDocumental(t)).isEqualTo("VENCIDO");
    }
}
