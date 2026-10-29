package ar.edu.grupo4.accesos.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;

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

    /**
     * Los XxxResponse siempre envían todos sus campos: se marcan obligatorios para que los tipos generados
     * en el frontend (npm run api:types) no sean opcionales. Si un campo puede venir null, declararlo en el
     * record con @Schema(types = {"string", "null"}) (o el tipo que corresponda).
     * Los XxxRequest no se tocan: lo obligatorio sale de Bean Validation (@NotNull, @NotBlank...).
     */
    @Bean
    public OpenApiCustomizer responsesConCamposObligatorios() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            openApi.getComponents().getSchemas().forEach(OpenApiConfig::marcarObligatorios);
        };
    }

    // Schema<?> y no Schema: el mapa de springdoc usa el tipo crudo, y con el comodín el compilador no avisa
    private static void marcarObligatorios(String nombre, Schema<?> schema) {
        if (nombre.endsWith("Response") && schema.getProperties() != null) {
            schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
        }
    }
}
