package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.OperadorRequest;
import ar.edu.grupo4.accesos.barrio.dto.OperadorResponse;
import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.usuario.Usuario;
import ar.edu.grupo4.accesos.usuario.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OperadorService {

    private final OperadorRepository operadorRepository;
    private final UsuarioService usuarioService;

    /**
     * US-13: crea la cuenta con rol OPERADOR y sus datos propios en la misma transacción. Queda "Disponible"
     * (sin barrio) hasta que se lo asigne (US-45) y no puede iniciar sesión hasta entonces (RN-15).
     */
    @Transactional
    public OperadorResponse crear(OperadorRequest request) {
        if (usuarioService.existePorEmail(request.email())) {
            throw new BusinessException("EMAIL_DUPLICADO", "Este email ya se encuentra registrado");
        }
        Usuario usuario = usuarioService.crearConRol(request.email(), request.password(), "OPERADOR");
        Operador operador = operadorRepository.save(new Operador(usuario.getId(), request.nombre().trim(),
            request.apellido().trim(), request.telefono().trim()));
        return OperadorResponse.from(operador, usuario.getEmail());
    }

    /** US-45: Operadores sin barrio asignado, ordenados por apellido y nombre. */
    @Transactional(readOnly = true)
    public List<OperadorResponse> listarDisponibles() {
        return operadorRepository.findDisponibles().stream()
            .map(o -> new OperadorResponse(o.getId(), o.getNombre(), o.getApellido(), o.getEmail(), o.getTelefono()))
            .toList();
    }

    @Transactional(readOnly = true)
    public boolean existe(Long operadorId) {
        return operadorRepository.existsById(operadorId);
    }

    /** Operadores por id de usuario, en una sola consulta (para mostrar el nombre del Operador de cada barrio). */
    @Transactional(readOnly = true)
    public Map<Long, Operador> buscarPorIds(Collection<Long> ids) {
        return operadorRepository.findAllById(ids).stream()
            .collect(Collectors.toMap(Operador::getUsuarioId, Function.identity()));
    }
}
