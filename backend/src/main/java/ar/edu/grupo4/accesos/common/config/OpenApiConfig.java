package ar.edu.grupo4.accesos.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contrato de la API (Swagger/OpenAPI) entre frontend y backend.
 * UI: /swagger-ui.html - JSON: /v3/api-docs. Deshabilitado en el perfil prod.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_JWT = "bearerJwt";

    @Bean
    public OpenAPI accesosOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Control de Accesos - Grupo 4")
                .description("API REST de la plataforma de control de accesos para barrios privados")
                .version("v1"))
            // Esquema JWT para el botón "Authorize" de Swagger (se usa desde US-04)
            .components(new Components().addSecuritySchemes(BEARER_JWT, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT));
    }
}
