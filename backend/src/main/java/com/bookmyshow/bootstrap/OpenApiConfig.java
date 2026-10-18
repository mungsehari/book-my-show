package com.bookmyshow.bootstrap;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;

/**
 * API docs. Swagger UI is served at {@code /swagger-ui.html}. Use "Authorize" with an access token from
 * /api/v1/auth/login to try admin APIs.
 */
public class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Book My Show API")
                        .version("v1")
                        .description("Browse cities, cinemas, movies, and shows. Log in. Manage cinemas as an admin."))
                .components(new Components()
                        .addSecuritySchemes(
                                "bearer",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
