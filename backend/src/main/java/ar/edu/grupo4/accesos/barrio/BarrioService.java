package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.BarrioRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BarrioService {

    private final BarrioRepository barrioRepository;

    /** US-10: el barrio nace Inactivo y sin Operador (RN-10). */
    @Transactional
    public Barrio crear(BarrioRequest request) {
        Barrio barrio = new Barrio(request.nombre().trim(), request.localidadProvincia().trim(),
            request.direccion().trim());
        return barrioRepository.save(barrio);
    }

    /** US-10: el listado de barrios no se pagina; orden alfabético. */
    @Transactional(readOnly = true)
    public List<Barrio> listar() {
        return barrioRepository.findAll(Sort.by("nombre"));
    }
}
