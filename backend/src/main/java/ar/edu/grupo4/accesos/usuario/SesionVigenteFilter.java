package ar.edu.grupo4.accesos.usuario;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * T-03: después de validar la firma del token, comprueba contra la base que la sesión siga vigente
 * (no cerró sesión) y que la cuenta pueda operar. Así un Guardia deshabilitado, un Operador reemplazado o un perfil
 * suspendido dejan de operar en la próxima petición, aunque su token no haya vencido (US-42 CA-4, US-43 CA-3).
 *
 * No es un @Component a propósito: Spring lo registraría también fuera de la cadena de seguridad.
 * Lo crea SecurityConfig.
 */
public class SesionVigenteFilter extends OncePerRequestFilter {

    private final UsuarioActual usuarioActual;
    private final HandlerExceptionResolver exceptionResolver;

    public SesionVigenteFilter(UsuarioActual usuarioActual, HandlerExceptionResolver exceptionResolver) {
        this.usuarioActual = usuarioActual;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Sin token (rutas públicas) no hay nada que validar
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken) {
            try {
                usuarioActual.validarSesion();
            } catch (RuntimeException e) {
                // Mismo formato de error que el resto de la API (GlobalExceptionHandler)
                SecurityContextHolder.clearContext();
                exceptionResolver.resolveException(request, response, null, e);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
