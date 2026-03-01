package com.liquilabs.vankoo.iam.interfaces.rest.transform;

public class ApiVersionParser implements org.springframework.web.accept.ApiVersionParser {

    // allows us to use /api/v2/users instead of /api/2.0/users
    @Override
    public Comparable parseVersion(String version) {
        if (version == null) {
            return null;
        }

        // Filtro para Scalar y recursos estáticos
        // Si el segmento coincide con estas palabras, no es una versión.
        if (isReservedWord(version)) {
            return null;
        }

        // Estilo springdoc-demo: Remove "v" or "V" prefix
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }

        return version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
    }

    private boolean isReservedWord(String version) {
        return version.equalsIgnoreCase("scalar") ||
                version.equalsIgnoreCase("swagger-ui") ||
                version.equalsIgnoreCase("index.html") ||
                version.equalsIgnoreCase("api-docs") ||
                version.equalsIgnoreCase("docs") ||
                version.endsWith(".html") ||
                version.endsWith(".js") ||
                version.endsWith(".css");
    }
}