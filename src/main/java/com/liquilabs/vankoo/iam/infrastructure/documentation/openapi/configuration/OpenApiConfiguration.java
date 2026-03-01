package com.liquilabs.vankoo.iam.infrastructure.documentation.openapi.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
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
                        .version("1.0.0"))
                .addServersItem(new io.swagger.v3.oas.models.servers.Server().url("/").description("Default Server"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("v1")
                .pathsToMatch("/api/v{version}/**")
                .addOpenApiCustomizer(cleanPathCustomizer()) // Lo aplicamos directamente al grupo
                .build();
    }

    @Bean
    public OpenApiCustomizer cleanPathCustomizer() {
        return openApi -> {
            Paths paths = openApi.getPaths();
            if (paths == null) return;

            Paths cleanPaths = new Paths();
            paths.forEach((pathKey, pathItem) -> {
                // Buscamos la versión en los Tags de las operaciones
                String detectedVersion = pathItem.readOperations().stream()
                        .flatMap(operation -> operation.getTags().stream())
                        .filter(tag -> tag.matches(".*V\\d+.*")) // Busca patrones como "V1", "V2", etc.
                        .map(tag -> tag.replaceAll(".*V(\\d+).*", "$1")) // Extrae solo el número
                        .findFirst()
                        .orElse("1"); // Si no encuentra nada, asume v1

                // Reemplazo dinámico basado en el Tag detectado
                String resolvedPath = pathKey.replace("{version}", detectedVersion);
                cleanPaths.addPathItem(resolvedPath, pathItem);
            });
            openApi.setPaths(cleanPaths);
        };
    }
}
