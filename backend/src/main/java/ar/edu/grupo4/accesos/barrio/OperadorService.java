package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.OperadorListadoResponse;
import ar.edu.grupo4.accesos.barrio.dto.OperadorModificarRequest;
import ar.edu.grupo4.accesos.barrio.dto.OperadorRequest;
import ar.edu.grupo4.accesos.barrio.dto.OperadorResponse;
import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.usuario.Usuario;
import ar.edu.grupo4.accesos.usuario.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
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
        log.info("Operador {} creado", operador.getUsuarioId());
        return OperadorResponse.from(operador, usuario.getEmail());
    }

    /** US-16: datos de un Operador con su email (solo lectura), para el formulario de edición. */
    @Transactional(readOnly = true)
    public OperadorResponse obtener(Long id) {
        Operador operador = operadorRepository.findById(id).orElseThrow(NotFoundException::new);
        return OperadorResponse.from(operador, emailDe(id));
    }

    /**
     * US-16 (RF-32): modifica nombre, apellido y teléfono de un Operador. El email no se toca: es su usuario de
     * acceso y la petición ni siquiera lo trae. No cambia su barrio ni su sesión.
     */
    @Transactional
    public OperadorResponse modificar(Long id, OperadorModificarRequest request) {
        Operador operador = operadorRepository.findById(id).orElseThrow(NotFoundException::new);
        operador.setNombre(request.nombre().trim());
        operador.setApellido(request.apellido().trim());
        operador.setTelefono(request.telefono().trim());
        Operador guardado = operadorRepository.save(operador);
        log.info("Operador {} modificado", id);
        return OperadorResponse.from(guardado, emailDe(id));
    }

    // El id del Operador es el de su cuenta (usuario): de ahí sale el email
    private String emailDe(Long id) {
        return usuarioService.buscarPorId(id).map(Usuario::getEmail).orElseThrow(NotFoundException::new);
    }

    /** US-45: Operadores sin barrio asignado, ordenados por apellido y nombre. */
    @Transactional(readOnly = true)
    public List<OperadorResponse> listarDisponibles() {
        return operadorRepository.findDisponibles().stream()
            .map(o -> new OperadorResponse(o.getId(), o.getNombre(), o.getApellido(), o.getEmail(), o.getTelefono()))
            .toList();
    }

    /** US-14: cierra las sesiones de un Operador que deja de tener barrio (el id del Operador es el de su cuenta). */
    @Transactional
    public void cerrarSesiones(Long operadorId) {
        usuarioService.cerrarSesiones(operadorId);
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

    /**
     * US-15: listado paginado de Operadores del Administrador, con su barrio. Filtros opcionales: estado (Disponible
     * si no tiene barrio, Ocupado si lo tiene) y texto, que se busca en nombre, apellido o email.
     */
    @Transactional(readOnly = true)
    public PageResponse<OperadorListadoResponse> listar(EstadoOperador estado, String q, Pageable pageable) {
        String texto = (q != null && !q.isBlank()) ? escaparLike(q.trim()) : null;
        return PageResponse.from(
            operadorRepository.listarConFiltros(estado == null ? null : estado.name(), texto, pageable),
            OperadorListadoResponse::from);
    }

    // Lo que escribe la persona se busca como texto: %, _ y la barra no actúan como comodines
    private String escaparLike(String texto) {
        return texto
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}
