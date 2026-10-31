package ar.edu.grupo4.accesos.usuario;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * T-03: quién hace la petición. Es el único lugar donde se resuelve el usuario, su barrio y su perfil, siempre desde
 * el token y la base de datos, nunca desde lo que manda el cliente (aislamiento RF-23 y RF-24).
 *
 * <h2>Cómo se usa</h2>
 * Se inyecta en el Service que lo necesita:
 * <pre>
 * Long barrioId = usuarioActual.barrioId();          // Operador o Guardia: solo su barrio (US-42)
 * Long perfilId = usuarioActual.perfilIdAprobado();  // Empresa o Monotributista aprobado (US-41, US-43)
 * </pre>
 *
 * <h2>De dónde sale cada dato</h2>
 * <ul>
 *   <li>Del <b>token</b> se usan solo el id del usuario (claim "sub") y la versión de sesión (claim "ver").
 *       Spring Security ya validó la firma y el vencimiento antes de llegar acá.</li>
 *   <li>Todo lo demás (estado de la cuenta, barrio, perfil aprobado) se lee de la <b>base</b>: el token dura 8 horas
 *       y en ese tiempo esos datos pueden cambiar (un Guardia deshabilitado, un perfil suspendido).</li>
 * </ul>
 *
 * <h2>Una instancia por petición</h2>
 * Con {@code @RequestScope}, Spring crea un UsuarioActual nuevo para cada petición HTTP y lo descarta al terminar.
 * Por eso puede guardar en un campo los datos que leyó de la base: dentro de la misma petición, el filtro de sesión,
 * el Service y cualquier otro que lo use comparten esa lectura (una sola consulta), y la petición siguiente vuelve a
 * leer datos frescos.
 */
@Component
@RequestScope
@RequiredArgsConstructor
public class UsuarioActual {

    private final UsuarioService usuarioService;

    /** Datos del usuario leídos de la base en esta petición. Null hasta que alguien los pide por primera vez. */
    private UsuarioRepository.DatosDeSesion datos;

    /** Id del usuario del token (claim "sub"). */
    public Long id() {
        return Long.valueOf(token().getSubject());
    }

    /**
     * Barrio del Guardia o el que tiene a cargo el Operador (US-42).
     * 403 si el usuario no tiene barrio (por ejemplo, un Administrador).
     */
    public Long barrioId() {
        Long barrioId = datos().getBarrioId();
        if (barrioId == null) {
            throw new BusinessException("SIN_BARRIO", "No tenés un barrio asignado.", HttpStatus.FORBIDDEN);
        }
        return barrioId;
    }

    /**
     * Perfil de Empresa o Monotributista, solo si está Aprobado (RN-14, US-43).
     * 403 si está pendiente, rechazado o suspendido, o si la cuenta no tiene perfil.
     */
    public Long perfilIdAprobado() {
        Long perfilId = datos().getPerfilAprobadoId();
        if (perfilId == null) {
            throw new BusinessException("PERFIL_NO_APROBADO",
                "Tu perfil no está aprobado. Contactá al Administrador de la plataforma.", HttpStatus.FORBIDDEN);
        }
        return perfilId;
    }

    /**
     * Lo llama SesionVigenteFilter en cada petición con token, antes de llegar al Controller:
     * <ul>
     *   <li>401 si la sesión se cerró después de emitir el token (US-05): la versión del token no coincide con la de
     *       la base, porque al cerrar sesión la base sumó 1. También si el token no trae versión (emitido antes de
     *       que existiera).</li>
     *   <li>403 con el mismo mensaje del login si la cuenta ya no puede operar: suspendida, Guardia deshabilitado,
     *       barrio inactivo u Operador sin barrio (US-42 CA-4, US-43 CA-3).</li>
     * </ul>
     */
    public void validarSesion() {
        Object versionDelToken = token().getClaim(TokenService.CLAIM_VERSION);
        boolean mismaVersion = versionDelToken instanceof Number numero
            && numero.intValue() == datos().getVersionSesion();
        if (!mismaVersion) {
            throw new BusinessException("SESION_CERRADA", "Tu sesión expiró. Iniciá sesión de nuevo.",
                HttpStatus.UNAUTHORIZED);
        }
        EstadoAcceso.valueOf(datos().getEstado()).exigirHabilitado();
    }

    /**
     * Datos del usuario en la base. La primera llamada de la petición hace la consulta y la guarda en el campo
     * {@code datos}; las siguientes devuelven lo guardado. Si la cuenta ya no existe, la sesión deja de valer (401).
     */
    private UsuarioRepository.DatosDeSesion datos() {
        if (datos == null) {
            datos = usuarioService.datosDeSesion(id())
                .orElseThrow(() -> new InsufficientAuthenticationException("La cuenta del token no existe"));
        }
        return datos;
    }

    /** Token de la petición, ya validado por Spring Security. 401 si la petición no trae token. */
    private static Jwt token() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken();
        }
        throw new InsufficientAuthenticationException("La petición no tiene un token de sesión");
    }
}
