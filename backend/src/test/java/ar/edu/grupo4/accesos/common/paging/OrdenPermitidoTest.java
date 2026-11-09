package ar.edu.grupo4.accesos.common.paging;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdenPermitidoTest {

    private static final Set<String> PERMITIDAS = Set.of("nombre", "apellido");

    @Test
    void sinOrdenPasaIgual() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThat(OrdenPermitido.validar(pageable, PERMITIDAS)).isSameAs(pageable);
    }

    @Test
    void ordenPorColumnasPermitidasPasa() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("nombre", "apellido"));

        assertThat(OrdenPermitido.validar(pageable, PERMITIDAS)).isSameAs(pageable);
    }

    @Test
    void ordenPorUnCampoDeOtraEntidadSeRechaza() {
        // ?sort=usuario.passwordHash ordenaría por el hash de la contraseña
        Pageable pageable = PageRequest.of(0, 10, Sort.by("usuario.passwordHash"));

        assertThatThrownBy(() -> OrdenPermitido.validar(pageable, PERMITIDAS))
            .isInstanceOf(BusinessException.class)
            .extracting("code").isEqualTo("ORDEN_INVALIDO");
    }

    @Test
    void ordenPorUnCampoQueNoExisteSeRechaza() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("foo"));

        assertThatThrownBy(() -> OrdenPermitido.validar(pageable, PERMITIDAS)).isInstanceOf(BusinessException.class);
    }
}
