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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BarrioService {

    static final String OPERADOR_OCUPADO = "El Operador ya está asignado a otro barrio";
    static final String BARRIO_DUPLICADO = "Ya existe un barrio con ese nombre en esa localidad/provincia";

    private final BarrioRepository barrioRepository;
    private final OperadorService operadorService;

    /** US-10: el barrio nace Inactivo y sin Operador (RN-10). No puede repetirse el nombre en la misma localidad/provincia. */
    @Transactional
    public Barrio crear(BarrioRequest request) {
        String nombre = request.nombre().trim();
        String localidad = request.localidadProvincia().trim();
        if (barrioRepository.existsByNombreIgnoreCaseAndLocalidadProvinciaIgnoreCase(nombre, localidad)) {
            throw barrioDuplicado();
        }
        return guardar(new Barrio(nombre, localidad, request.direccion().trim()));
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

    /** Operadores a cargo de los barrios dados, por id de Operador (una sola consulta; los barrios sin Operador no aportan). */
    @Transactional(readOnly = true)
    public Map<Long, Operador> operadoresDe(Collection<Barrio> barrios) {
        List<Long> ids = barrios.stream().map(Barrio::getOperadorId).filter(Objects::nonNull).distinct().toList();
        return ids.isEmpty() ? new HashMap<>() : operadorService.buscarPorIds(ids);
    }

    /**
     * US-11: modifica nombre, localidad/provincia y dirección. No toca el estado ni el Operador
     * (el Operador se asigna por US-45 y se reemplaza por US-14; el estado, por US-12).
     */
    @Transactional
    public Barrio modificar(Long id, BarrioRequest request) {
        Barrio barrio = obtener(id);
        String nombre = request.nombre().trim();
        String localidad = request.localidadProvincia().trim();
        if (barrioRepository.existsByNombreIgnoreCaseAndLocalidadProvinciaIgnoreCaseAndIdNot(nombre, localidad, id)) {
            throw barrioDuplicado();
        }
        barrio.setNombre(nombre);
        barrio.setLocalidadProvincia(localidad);
        barrio.setDireccion(request.direccion().trim());
        return guardar(barrio);
    }

    /** Guarda y fuerza el INSERT/UPDATE: si dos pedidos iguales llegan a la vez, el índice único de la base frena al segundo. */
    private Barrio guardar(Barrio barrio) {
        try {
            return barrioRepository.saveAndFlush(barrio);
        } catch (DataIntegrityViolationException e) {
            throw barrioDuplicado();
        }
    }

    private static BusinessException barrioDuplicado() {
        return new BusinessException("BARRIO_DUPLICADO", BARRIO_DUPLICADO);
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
                "El barrio ya tiene un Operador asignado. Para cambiarlo, reemplazalo.");
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
