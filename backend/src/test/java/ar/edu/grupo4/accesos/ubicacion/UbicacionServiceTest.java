package ar.edu.grupo4.accesos.ubicacion;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** T-11: consulta del catálogo de provincias y localidades. */
class UbicacionServiceTest {

    private static final Provincia BUENOS_AIRES = new Provincia("06", "Buenos Aires");

    private final ProvinciaRepository provincias = mock(ProvinciaRepository.class);
    private final LocalidadRepository localidades = mock(LocalidadRepository.class);
    private final UbicacionService service = new UbicacionService(provincias, localidades);

    @Test
    void listaLasProvinciasEnOrdenAlfabetico() {
        List<Provincia> lista = List.of(BUENOS_AIRES);
        when(provincias.findAll(Sort.by("nombre"))).thenReturn(lista);

        assertThat(service.listarProvincias()).isSameAs(lista);
    }

    @Test
    void listaLasLocalidadesDeUnaProvincia() {
        List<Localidad> lista = List.of(new Localidad("06070010", "Baradero", "Baradero", BUENOS_AIRES));
        when(provincias.existsById("06")).thenReturn(true);
        when(localidades.findByProvinciaIdOrderByNombreAscDepartamentoAsc("06")).thenReturn(lista);

        assertThat(service.listarLocalidades("06")).isSameAs(lista);
    }

    @Test
    void unaProvinciaQueNoExisteDevuelveNotFound() {
        when(provincias.existsById("99")).thenReturn(false);

        assertThatThrownBy(() -> service.listarLocalidades("99"))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("La provincia no existe.");
    }

    @Test
    void unaLocalidadQueNoEstaEnElCatalogoSeRechaza() {
        when(localidades.findById("99999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerLocalidad("99999999"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("La localidad no existe");
    }
}
