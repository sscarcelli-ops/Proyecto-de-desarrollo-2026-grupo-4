package ar.edu.grupo4.accesos.guardia;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.common.security.PoliticaPassword;
import ar.edu.grupo4.accesos.guardia.dto.GuardiaModificarRequest;
import ar.edu.grupo4.accesos.guardia.dto.GuardiaRequest;
import ar.edu.grupo4.accesos.guardia.dto.GuardiaResponse;
import ar.edu.grupo4.accesos.usuario.Usuario;
import ar.edu.grupo4.accesos.usuario.UsuarioActual;
import ar.edu.grupo4.accesos.usuario.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * US-32: el Operador gestiona los Guardias de SU barrio (RF-14, RN-09). El barrio sale siempre de UsuarioActual,
 * nunca del body ni de la URL (US-42). El Guardia deshabilitado no puede iniciar sesión ni operar: lo controla
 * SesionVigenteFilter en cada petición, así que acá solo se cambia el estado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuardiaService {

    private final GuardiaRepository guardiaRepository;
    private final UsuarioService usuarioService;
    private final UsuarioActual usuarioActual;

    /** Crea la cuenta con rol GUARDIA y sus datos propios en la misma transacción. Queda habilitado. */
    @Transactional
    public GuardiaResponse crear(GuardiaRequest request) {
        Long barrioId = usuarioActual.barrioId();
        if (usuarioService.existePorEmail(request.email().trim())) {
            throw new BusinessException("EMAIL_DUPLICADO", "Este email ya se encuentra registrado");
        }
        Usuario usuario = usuarioService.crearConRol(request.email(), request.password(), "GUARDIA");
        Guardia guardia = guardiaRepository.save(
            new Guardia(usuario.getId(), barrioId, request.nombre().trim(), request.apellido().trim()));
        log.info("Guardia {} creado en el barrio {}", guardia.getUsuarioId(), barrioId);
        return GuardiaResponse.from(guardia, usuario.getEmail());
    }

    @Transactional(readOnly = true)
    public List<GuardiaResponse> listar() {
        List<Guardia> guardias = guardiaRepository.findByBarrioIdOrderByApellidoAscNombreAsc(usuarioActual.barrioId());
        Map<Long, Usuario> cuentas = usuarioService.buscarPorIds(guardias.stream().map(Guardia::getUsuarioId).toList());
        return guardias.stream()
            .map(g -> GuardiaResponse.from(g, cuentas.get(g.getUsuarioId()).getEmail()))
            .toList();
    }

    @Transactional(readOnly = true)
    public GuardiaResponse obtener(Long id) {
        Guardia guardia = buscarDelBarrio(id);
        return GuardiaResponse.from(guardia, emailDe(guardia));
    }

    @Transactional
    public GuardiaResponse modificar(Long id, GuardiaModificarRequest request) {
        Guardia guardia = buscarDelBarrio(id);
        String password = request.password();
        boolean cambiaPassword = password != null && !password.isBlank();
        if (cambiaPassword) {
            validarPassword(password);
        }
        Usuario cuenta = usuarioService.actualizarCuenta(id, request.email(), cambiaPassword ? password : null);
        guardia.setNombre(request.nombre().trim());
        guardia.setApellido(request.apellido().trim());
        log.info("Guardia {} modificado (contraseña cambiada: {})", id, cambiaPassword);
        return GuardiaResponse.from(guardia, cuenta.getEmail());
    }

    /** Baja lógica y reversible (RN-09): se conserva el registro y el email sigue ocupado. */
    @Transactional
    public GuardiaResponse cambiarEstado(Long id, boolean habilitado) {
        Guardia guardia = buscarDelBarrio(id);
        guardia.setHabilitado(habilitado);
        log.info("Guardia {} {}", id, habilitado ? "habilitado" : "deshabilitado");
        return GuardiaResponse.from(guardia, emailDe(guardia));
    }

    // Un Guardia de otro barrio no se revela: 404, igual que si no existiera (US-42)
    private Guardia buscarDelBarrio(Long id) {
        return guardiaRepository.findByUsuarioIdAndBarrioId(id, usuarioActual.barrioId())
            .orElseThrow(NotFoundException::new);
    }

    private String emailDe(Guardia guardia) {
        return usuarioService.buscarPorId(guardia.getUsuarioId()).orElseThrow(NotFoundException::new).getEmail();
    }

    private void validarPassword(String password) {
        if (PoliticaPassword.esDemasiadoLarga(password)) {
            throw new BusinessException("PASSWORD_INVALIDA", PoliticaPassword.MENSAJE_LARGO);
        }
        if (!PoliticaPassword.cumpleRequisitos(password)) {
            throw new BusinessException("PASSWORD_INVALIDA", PoliticaPassword.MENSAJE);
        }
    }
}
