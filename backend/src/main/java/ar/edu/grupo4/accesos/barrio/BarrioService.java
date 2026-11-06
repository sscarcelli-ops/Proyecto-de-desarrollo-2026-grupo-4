package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.AsignarOperadorRequest;
import ar.edu.grupo4.accesos.barrio.dto.BarrioRequest;
import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BarrioService {

    static final String OPERADOR_OCUPADO = "El Operador ya está asignado a otro barrio";

    private final BarrioRepository barrioRepository;
    private final OperadorService operadorService;

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

    /** US-11: datos de un barrio para precargar el formulario de edición. */
    @Transactional(readOnly = true)
    public Barrio obtener(Long id) {
        return barrioRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("El barrio no existe."));
    }

    /**
     * US-11: modifica nombre, localidad/provincia y dirección. No toca el estado ni el Operador
     * (el Operador se asigna por US-45 y se reemplaza por US-14; el estado, por US-12).
     */
    @Transactional
    public Barrio modificar(Long id, BarrioRequest request) {
        Barrio barrio = obtener(id);
        barrio.setNombre(request.nombre().trim());
        barrio.setLocalidadProvincia(request.localidadProvincia().trim());
        barrio.setDireccion(request.direccion().trim());
        return barrioRepository.save(barrio);
    }

    /**
     * US-45: asigna un Operador a un barrio sin Operador y lo deja Activo (RF-34, RN-10), todo en una transacción.
     * El Operador puede ser uno disponible o uno nuevo, que se crea en la misma operación (si algo falla, no queda
     * creado). Si otro Administrador lo asignó mientras tanto, no se asigna y se informa.
     */
    @Transactional
    public Barrio asignarOperador(Long barrioId, AsignarOperadorRequest request) {
        boolean conOperadorExistente = request.operadorId() != null;
        boolean conOperadorNuevo = request.nuevoOperador() != null;
        if (!conOperadorExistente && !conOperadorNuevo) {
            throw new BusinessException("OPERADOR_REQUERIDO", "Debe seleccionar un Operador");
        }
        if (conOperadorExistente && conOperadorNuevo) {
            throw new BusinessException("OPERADOR_AMBIGUO", "Indicá un Operador existente o uno nuevo, no ambos.");
        }

        Barrio barrio = barrioRepository.findByIdForUpdate(barrioId)
            .orElseThrow(() -> new NotFoundException("El barrio no existe."));
        if (barrio.getOperadorId() != null) {
            throw new BusinessException("BARRIO_CON_OPERADOR",
                "El barrio ya tiene un Operador asignado. Para cambiarlo, reemplácelo.");
        }

        Long operadorId;
        if (conOperadorExistente) {
            operadorId = request.operadorId();
            if (!operadorService.existe(operadorId)) {
                throw new NotFoundException("El Operador no existe.");
            }
            if (barrioRepository.existsByOperadorId(operadorId)) {
                throw new BusinessException("OPERADOR_OCUPADO", OPERADOR_OCUPADO);
            }
        } else {
            operadorId = operadorService.crear(request.nuevoOperador()).id();
        }

        barrio.setOperadorId(operadorId);
        barrio.setEstado(EstadoBarrio.ACTIVO);
        try {
            return barrioRepository.saveAndFlush(barrio);
        } catch (DataIntegrityViolationException e) {
            // Otro Administrador asignó ese Operador entre la verificación y el guardado (la base lo impide: UNIQUE)
            throw new BusinessException("OPERADOR_OCUPADO", OPERADOR_OCUPADO);
        }
    }
}
