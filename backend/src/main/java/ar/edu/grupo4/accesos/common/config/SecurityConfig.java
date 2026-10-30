package ar.edu.grupo4.accesos.common.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Permite usar @PreAuthorize("hasRole('ADMIN')") en los controladores/servicios
public class SecurityConfig {

    private static final PathPatternRequestMatcher.Builder RUTA = PathPatternRequestMatcher.withDefaults();

    // Rutas sin sesión, en un solo lugar: se permiten sin token y en ellas el token se ignora (ver bearerTokenResolver).
    // Cambiar contraseña (US-07) NO va acá: necesita sesión. Recupero (US-06) sí, cuando exista.
    private static final RequestMatcher RUTAS_PUBLICAS = new OrRequestMatcher(
        RUTA.matcher("/api/ping"),
        RUTA.matcher("/api/health"),
        RUTA.matcher("/v3/api-docs/**"),
        RUTA.matcher("/swagger-ui/**"),
        RUTA.matcher("/swagger-ui.html"),
        RUTA.matcher(HttpMethod.POST, "/api/auth/login"),
        RUTA.matcher(HttpMethod.POST, "/api/auth/registro"),
        RUTA.matcher(HttpMethod.POST, "/api/auth/activacion"),
        RUTA.matcher(HttpMethod.POST, "/api/auth/activacion/validar")
    );

    // Spring convierte la lista separada por comas en List<String>
    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

    // RF-21: contraseñas con hash bcrypt (mismo formato que los usuarios del seed)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // Deshabilitado para APIs REST stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 401 (sin token o token inválido) y 403 (sin permiso) salen por GlobalExceptionHandler,
            // con el mismo formato que el resto de los errores. El frontend usa el 401 para descartar el token.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) -> exceptionResolver.resolveException(request, response, null, e))
                .accessDeniedHandler((request, response, e) -> exceptionResolver.resolveException(request, response, null, e)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(RUTAS_PUBLICAS).permitAll()
                .anyRequest().authenticated()
            )
            // Habilitamos el Resource Server para validar tokens Bearer JWT automáticamente
            .oauth2ResourceServer(oauth2 -> oauth2
                .bearerTokenResolver(tokenSoloEnRutasProtegidas())
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint((request, response, e) -> exceptionResolver.resolveException(request, response, null, e))
            );

        return http.build();
    }

    /**
     * Toma el token del header Authorization: Bearer, salvo en las rutas públicas. Sin esto, un token vencido
     * que el navegador todavía tenga guardado haría fallar con 401 hasta el propio login.
     */
    private static BearerTokenResolver tokenSoloEnRutasProtegidas() {
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        return request -> RUTAS_PUBLICAS.matches(request) ? null : resolver.resolve(request);
    }

    /**
     * Mapea el claim "roles" del JWT a autoridades de Spring Security (con prefijo ROLE_)
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // El JWT viaja en el header Authorization, no en cookies: no hacen falta credenciales
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}