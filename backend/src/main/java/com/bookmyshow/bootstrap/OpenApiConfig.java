package com.bookmyshow.bootstrap;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;

/** API docs. Swagger UI is served at {@code /swagger-ui.html}. */
public class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Book My Show API")
                        .version("v1")
                        .description("Browse cities, cinemas, and shows."));
    }
}
