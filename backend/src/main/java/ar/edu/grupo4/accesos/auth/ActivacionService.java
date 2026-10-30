package ar.edu.grupo4.accesos.auth;

import ar.edu.grupo4.accesos.auth.dto.ActivacionRequest;
import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.usuario.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * US-01: activación del Administrador inicial con el código del manual de instalación (RF-01).
 * El código se carga desde la configuración al arrancar, se valida en "Activación de cuenta" sin consumirse
 * y se consume al crear la cuenta, en la misma transacción.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivacionService {

    static final String TOKEN_UTILIZADO = "Este código de activación ya fue utilizado. "
        + "Si ya configuró su cuenta, por favor inicie sesión con sus credenciales";
    // Un solo mensaje para inexistente y vencido: no revela si el código existió
    static final String TOKEN_INVALIDO = "El código de activación no es válido o ha expirado. "
        + "Por favor, comuníquese con el soporte técnico para solicitar uno nuevo.";

    private final TokenUnSoloUsoRepository tokenRepository;
    private final UsuarioService usuarioService;

    /** CA-1, CA-3 y CA-4: el código sirve para pasar a "Registrate". No lo consume. */
    @Transactional(readOnly = true)
    public void validar(String codigo) {
        buscarVigente(codigo);
    }

    /**
     * CA-2: crea el Administrador y consume el código. Si algo falla (email repetido, otro lo usó recién),
     * no se crea la cuenta y el código queda como estaba (CA-5, CA-6).
     */
    @Transactional
    public void activar(ActivacionRequest request) {
        TokenUnSoloUso token = buscarVigente(request.token());
        if (usuarioService.existePorEmail(request.email())) {
            throw new BusinessException("EMAIL_DUPLICADO", "Este email ya se encuentra registrado");
        }
        // CA-6: si otra persona lo consumió mientras completaba el registro, no se marca y no se crea la cuenta
        if (tokenRepository.marcarUsado(token.getId(), Instant.now()) == 0) {
            throw new BusinessException("TOKEN_UTILIZADO", TOKEN_UTILIZADO);
        }
        usuarioService.crearConRol(request.email(), request.password(), "ADMIN");
        log.info("Administrador inicial creado con el código de activación");
    }

    /**
     * Carga el código configurado (ACTIVACION_TOKEN) como el único vigente. No hace nada si está vacío, si ya hay
     * un Administrador o si ese mismo código ya se cargó antes (aunque esté usado o vencido: para uno nuevo,
     * soporte cambia el valor y reinicia).
     */
    @Transactional
    public void cargarCodigoConfigurado(String codigo, int vigenciaDias) {
        if (codigo == null || codigo.isBlank()) {
            log.info("Sin código de activación configurado (ACTIVACION_TOKEN vacío)");
            return;
        }
        if (usuarioService.existeAdministrador()) {
            log.info("Ya existe un Administrador: no se carga el código de activación");
            return;
        }
        String hash = TokenUnSoloUso.hashDe(codigo);
        if (tokenRepository.findByTipoAndTokenHash(TipoTokenUnSoloUso.ACTIVACION_ADMIN, hash).isPresent()) {
            return;
        }
        Instant ahora = Instant.now();
        tokenRepository.vencerVigentes(TipoTokenUnSoloUso.ACTIVACION_ADMIN, ahora);

        TokenUnSoloUso token = new TokenUnSoloUso();
        token.setTipo(TipoTokenUnSoloUso.ACTIVACION_ADMIN);
        token.setTokenHash(hash);
        token.setExpiraEn(ahora.plus(Duration.ofDays(vigenciaDias)));
        tokenRepository.save(token);
        log.info("Código de activación del Administrador cargado. Vence el {}", token.getExpiraEn());
    }

    private TokenUnSoloUso buscarVigente(String codigo) {
        TokenUnSoloUso token = tokenRepository
            .findByTipoAndTokenHash(TipoTokenUnSoloUso.ACTIVACION_ADMIN, TokenUnSoloUso.hashDe(codigo))
            .orElseThrow(() -> new BusinessException("TOKEN_INVALIDO", TOKEN_INVALIDO));
        if (token.fueUsado()) {
            throw new BusinessException("TOKEN_UTILIZADO", TOKEN_UTILIZADO);
        }
        if (token.estaVencido(Instant.now())) {
            throw new BusinessException("TOKEN_INVALIDO", TOKEN_INVALIDO);
        }
        return token;
    }
}
