package com.liquilabs.vankoo.iam.infrastructure.documentation.openapi.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI iamServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Vankoo IAM Service API")
                        .description("API documentation for Vankoo IAM Service")
                        .version("1.0.0"));
    }
}
