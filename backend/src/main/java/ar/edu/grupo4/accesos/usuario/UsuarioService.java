package ar.edu.grupo4.accesos.usuario;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Acceso a las cuentas para los demás módulos (auth, guardia...): nunca usan el Repository directo. */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email.trim());
    }

    @Transactional(readOnly = true)
    public boolean existePorEmail(String email) {
        return usuarioRepository.existsByEmailIgnoreCase(email.trim());
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findWithRolesById(id);
    }

    /**
     * Si la cuenta puede operar (perfil no suspendido, Guardia habilitado, barrio del Guardia u Operador activo,
     * Operador con barrio asignado).
     * La usa el login (US-04 CA-3) y la va a usar el chequeo en cada petición (US-43, T-03).
     */
    @Transactional(readOnly = true)
    public EstadoAcceso estadoDeAcceso(Long usuarioId) {
        return EstadoAcceso.valueOf(usuarioRepository.estadoDeAcceso(usuarioId));
    }

    /** Nombre para mostrar en la interfaz; vacío para el Administrador o una cuenta sin perfil. */
    @Transactional(readOnly = true)
    public Optional<String> nombreVisible(Long usuarioId) {
        return Optional.ofNullable(usuarioRepository.nombreVisible(usuarioId));
    }

    /**
     * US-02: crea un usuario base sin rol, con contraseña hasheada con bcrypt (RF-08, RN-12).
     */
    @Transactional
    public Usuario crearUsuario(String email, String password) {
        String emailLimpio = email.trim();
        if (usuarioRepository.existsByEmailIgnoreCase(emailLimpio)) {
            throw new BusinessException(
                "EMAIL_DUPLICADO", "Este email ya se encuentra registrado", HttpStatus.BAD_REQUEST);
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(emailLimpio);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        return usuarioRepository.save(usuario);
    }
}
