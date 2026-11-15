package ar.edu.grupo4.accesos.auth;

import ar.edu.grupo4.accesos.common.correo.CorreoService;
import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.usuario.Usuario;
import ar.edu.grupo4.accesos.usuario.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * US-06: recuperar la contraseña con un enlace temporal y de un solo uso enviado al email registrado (RF-26).
 * <ul>
 *   <li>Pedir el enlace responde siempre igual, exista o no el email (CA-2): no revela qué cuentas existen.</li>
 *   <li>El enlace lleva un código al azar de 256 bits; en la base se guarda solo su hash (tabla token, como la
 *       activación de US-01). Vence a los RECUPERO_VIGENCIA_MINUTOS y pedir otro vence el anterior.</li>
 *   <li>Usarlo define la contraseña nueva (política de US-44), lo consume y cierra las sesiones abiertas.</li>
 *   <li>No mira el estado de la cuenta: un Guardia deshabilitado cambia su contraseña pero sigue sin poder iniciar
 *       sesión, porque eso lo controla el login (CA-5).</li>
 * </ul>
 */
@Service
@Slf4j
public class RecuperoService {

    static final String ENLACE_INVALIDO = "El enlace de recuperación no es válido o ha vencido";
    static final String ASUNTO = "Recuperar tu contraseña - Control de Accesos";

    private static final SecureRandom AZAR = new SecureRandom();

    private final TokenUnSoloUsoRepository tokenRepository;
    private final UsuarioService usuarioService;
    private final CorreoService correoService;
    private final String urlFrontend;
    private final int vigenciaMinutos;

    public RecuperoService(TokenUnSoloUsoRepository tokenRepository,
                           UsuarioService usuarioService,
                           CorreoService correoService,
                           @Value("${app.url-frontend}") String urlFrontend,
                           @Value("${app.recupero.vigencia-minutos}") int vigenciaMinutos) {
        this.tokenRepository = tokenRepository;
        this.usuarioService = usuarioService;
        this.correoService = correoService;
        this.urlFrontend = urlFrontend.replaceAll("/+$", "");
        this.vigenciaMinutos = vigenciaMinutos;
    }

    /**
     * CA-1 y CA-2: si el email es de una cuenta, le manda el enlace; si no, no hace nada. Quien llama responde lo
     * mismo en los dos casos.
     */
    @Transactional
    public void solicitar(String email) {
        Optional<Usuario> usuario = usuarioService.buscarPorEmail(email);
        if (usuario.isEmpty()) {
            log.info("Recupero de contraseña pedido para un email no registrado: no se envía nada");
            return;
        }
        Long usuarioId = usuario.get().getId();
        String codigo = nuevoCodigo();
        Instant ahora = Instant.now();
        tokenRepository.vencerVigentesDeUsuario(TipoTokenUnSoloUso.RECUPERO, usuarioId, ahora);

        TokenUnSoloUso token = new TokenUnSoloUso();
        token.setTipo(TipoTokenUnSoloUso.RECUPERO);
        token.setUsuarioId(usuarioId);
        token.setTokenHash(TokenUnSoloUso.hashDe(codigo));
        token.setExpiraEn(ahora.plus(Duration.ofMinutes(vigenciaMinutos)));
        tokenRepository.save(token);

        String enlace = "%s/restablecer-contrasena?token=%s".formatted(urlFrontend, codigo);
        boolean enviado = correoService.enviar(usuario.get().getEmail(), ASUNTO, cuerpo(enlace), cuerpoHtml(enlace));
        log.info("Recupero de contraseña pedido por el usuario {}: enlace {}", usuarioId,
            enviado ? "enviado por correo" : "sin enviar (ver el aviso anterior)");
    }

    /** Al abrir la pantalla del enlace: si sirve. No lo consume (CA-4 se muestra antes de escribir nada). */
    @Transactional(readOnly = true)
    public void validar(String codigo) {
        buscarVigente(codigo);
    }

    /**
     * CA-3 y CA-4: define la contraseña nueva y consume el enlace. Si otro pedido lo usó recién (dos pestañas), la
     * base deja pasar solo uno y el otro recibe el error del enlace.
     */
    @Transactional
    public void restablecer(String codigo, String passwordNueva) {
        TokenUnSoloUso token = buscarVigente(codigo);
        if (tokenRepository.marcarUsado(token.getId(), Instant.now()) == 0) {
            throw enlaceInvalido();
        }
        usuarioService.restablecerPassword(token.getUsuarioId(), passwordNueva);
        log.info("Contraseña restablecida con el enlace de recupero por el usuario {}: se cierran sus sesiones",
            token.getUsuarioId());
    }

    private TokenUnSoloUso buscarVigente(String codigo) {
        TokenUnSoloUso token = tokenRepository
            .findByTipoAndTokenHash(TipoTokenUnSoloUso.RECUPERO, TokenUnSoloUso.hashDe(codigo))
            .orElseThrow(this::enlaceInvalido);
        // Un solo mensaje para usado y vencido: no se distingue (CA-4)
        if (token.fueUsado() || token.estaVencido(Instant.now())) {
            throw enlaceInvalido();
        }
        return token;
    }

    private BusinessException enlaceInvalido() {
        log.warn("Enlace de recupero de contraseña rechazado: no existe, ya se usó o venció");
        return new BusinessException("ENLACE_INVALIDO", ENLACE_INVALIDO);
    }

    /** 256 bits al azar en hexadecimal: no se puede adivinar y hashDe() lo guarda igual que lo recibe. */
    private static String nuevoCodigo() {
        byte[] bytes = new byte[32];
        AZAR.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** Versión de texto: la ven los programas de correo sin HTML y es la que va al log si no hay servidor. */
    private String cuerpo(String enlace) {
        return """
            Hola:

            Recibimos un pedido para restablecer la contraseña de tu cuenta de Control de Accesos.
            Para elegir una nueva, abrí este enlace:

            %s

            El enlace vence en %d minutos y sirve una sola vez. Si no fuiste vos, ignorá este correo: tu contraseña no cambia.
            """.formatted(enlace, vigenciaMinutos);
    }

    /** Versión HTML: el mismo texto con un botón. Estilos en línea, porque los programas de correo ignoran el CSS aparte. */
    private String cuerpoHtml(String enlace) {
        return """
            <div style="font-family:Arial,Helvetica,sans-serif;color:#1a2233;max-width:480px;margin:0 auto;padding:24px">
              <h2 style="margin:0 0 16px">Restablecer contraseña</h2>
              <p>Hola:</p>
              <p>Recibimos un pedido para restablecer la contraseña de tu cuenta de Control de Accesos.</p>
              <p style="margin:28px 0">
                <a href="%1$s" style="background:#1f4e9c;color:#ffffff;padding:12px 22px;border-radius:6px;\
            text-decoration:none;font-weight:bold;display:inline-block">Elegir una nueva contraseña</a>
              </p>
              <p style="color:#5b6475;font-size:13px">El enlace vence en %2$d minutos y sirve una sola vez. Si no fuiste vos,
              ignorá este correo: tu contraseña no cambia.</p>
              <p style="color:#5b6475;font-size:12px">Si el botón no funciona, copiá esta dirección en el navegador:<br>
              <a href="%1$s" style="color:#1f4e9c;word-break:break-all">%1$s</a></p>
            </div>
            """.formatted(enlace, vigenciaMinutos);
    }
}
