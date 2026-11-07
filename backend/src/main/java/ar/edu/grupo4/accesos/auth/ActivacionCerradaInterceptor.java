package ar.edu.grupo4.accesos.auth;

import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * US-01: con el Administrador ya creado, la activación deja de existir. Todos los endpoints de /api/auth/activacion
 * responden el mismo 404 que una ruta inexistente, antes de leer el pedido: no se valida el cuerpo ni se compara
 * ningún código, así no queda un campo abierto para probar códigos (pedido del profesor: "mientras más dejes abierto,
 * más posibilidades de ataque").
 */
@Configuration
@RequiredArgsConstructor
public class ActivacionCerradaInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    private final ActivacionService activacionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!activacionService.disponible()) {
            throw new NotFoundException();
        }
        return true;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/api/auth/activacion", "/api/auth/activacion/**");
    }
}
