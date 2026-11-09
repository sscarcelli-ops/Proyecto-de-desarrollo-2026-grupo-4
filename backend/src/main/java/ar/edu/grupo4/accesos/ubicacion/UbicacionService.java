package ar.edu.grupo4.accesos.ubicacion;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** T-11: consulta del catálogo de provincias y localidades. Los demás módulos lo usan desde acá. */
@Service
@RequiredArgsConstructor
public class UbicacionService {

    private final ProvinciaRepository provinciaRepository;
    private final LocalidadRepository localidadRepository;

    /** Las 24 provincias, en orden alfabético. */
    @Transactional(readOnly = true)
    public List<Provincia> listarProvincias() {
        return provinciaRepository.findAll(Sort.by("nombre"));
    }

    /** Las localidades de una provincia, en orden alfabético (y por partido si el nombre se repite). */
    @Transactional(readOnly = true)
    public List<Localidad> listarLocalidades(String provinciaId) {
        if (!provinciaRepository.existsById(provinciaId)) {
            throw new NotFoundException("La provincia no existe.");
        }
        return localidadRepository.findByProvinciaIdOrderByNombreAscDepartamentoAsc(provinciaId);
    }

    /** La localidad elegida en un formulario. Si el id no es del catálogo, se rechaza. */
    @Transactional(readOnly = true)
    public Localidad obtenerLocalidad(String id) {
        return localidadRepository.findById(id)
            .orElseThrow(() -> new BusinessException("LOCALIDAD_INEXISTENTE", "La localidad no existe"));
    }
}
