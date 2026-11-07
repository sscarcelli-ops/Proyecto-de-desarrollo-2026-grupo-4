package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.BarrioRequest;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.ubicacion.UbicacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BarrioService {

    private final BarrioRepository barrioRepository;
    private final UbicacionService ubicacionService;

    /** US-10: el barrio nace Inactivo y sin Operador (RN-10). */
    @Transactional
    public Barrio crear(BarrioRequest request) {
        Barrio barrio = new Barrio(request.nombre().trim(), ubicacionService.obtenerLocalidad(request.localidadId()),
            request.direccion().trim());
        return barrioRepository.save(barrio);
    }

    /** US-10: el listado de barrios no se pagina; orden alfabético. */
    @Transactional(readOnly = true)
    public List<Barrio> listar() {
        return barrioRepository.findAll(Sort.by("nombre"));
    }

    /** US-11: datos de un barrio para precargar el formulario de edición. */
    @Transactional(readOnly = true)
    public Barrio obtener(Long id) {
        return barrioRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("El barrio no existe."));
    }

    /**
     * US-11: modifica nombre, localidad (T-11) y dirección. No toca el estado ni el Operador
     * (el Operador se asigna por US-45 y se reemplaza por US-14; el estado, por US-12).
     */
    @Transactional
    public Barrio modificar(Long id, BarrioRequest request) {
        Barrio barrio = obtener(id);
        barrio.setNombre(request.nombre().trim());
        barrio.setLocalidad(ubicacionService.obtenerLocalidad(request.localidadId()));
        barrio.setDireccion(request.direccion().trim());
        return barrioRepository.save(barrio);
    }
}
