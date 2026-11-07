package ar.edu.grupo4.accesos.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * T-10: una línea de log por cada pedido a la API, con método, ruta, estado y duración:
 * {@code GET /api/barrios -> 200 (12 ms)}.
 *
 * Además le da un número a cada pedido ("peticion" en el MDC) que aparece en todas las líneas de log de ese pedido,
 * y SesionVigenteFilter agrega el usuario del token ("usuario"). El formato está en application.yml.
 *
 * Va antes que Spring Security, para registrar también los pedidos rechazados (401 y 403). Esos van como WARN: son los
 * que hay que mirar ante un intento de entrar sin sesión o a datos de otro barrio. La ruta va sin los parámetros
 * (?dni=...), que pueden tener datos personales.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RegistroPeticionesFilter extends OncePerRequestFilter {

    public static final String MDC_PETICION = "peticion";
    public static final String MDC_USUARIO = "usuario";

    private static final Logger log = LoggerFactory.getLogger(RegistroPeticionesFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Solo la API: Swagger y los archivos estáticos no aportan
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long inicio = System.nanoTime();
        MDC.put(MDC_PETICION, UUID.randomUUID().toString().substring(0, 8));
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            int estado = response.getStatus();
            String linea = "{} {} -> {} ({} ms)";
            if (estado == HttpServletResponse.SC_UNAUTHORIZED || estado == HttpServletResponse.SC_FORBIDDEN) {
                log.warn(linea, request.getMethod(), request.getRequestURI(), estado, ms);
            } else {
                log.info(linea, request.getMethod(), request.getRequestURI(), estado, ms);
            }
            MDC.remove(MDC_USUARIO);
            MDC.remove(MDC_PETICION);
        }
    }
}
