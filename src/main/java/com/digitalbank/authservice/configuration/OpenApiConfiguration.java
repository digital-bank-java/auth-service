package com.digitalbank.authservice.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI authServiceOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Digital Bank Authentication and Session Service API")
                                .version("1.0.0")
                                .description(
                                        "Internal authentication, session, and authorization API for the Digital Bank Java platform."));
    }
}
