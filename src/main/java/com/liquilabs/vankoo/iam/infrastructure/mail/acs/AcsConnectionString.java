package com.liquilabs.vankoo.iam.infrastructure.mail.acs;

import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * La cadena de conexión de Azure Communication Services:
 * {@code endpoint=https://…;accesskey=…}. La clave viene en base64.
 */
public record AcsConnectionString(URI endpoint, byte[] accessKey) {

    public static AcsConnectionString parse(String value) {
        Map<String, String> parts = new HashMap<>();
        for (String part : value == null ? new String[0] : value.split(";")) {
            int index = part.indexOf('=');
            if (index > 0) {
                parts.put(part.substring(0, index).trim().toLowerCase(Locale.ROOT), part.substring(index + 1).trim());
            }
        }
        String endpoint = parts.get("endpoint");
        String accessKey = parts.get("accesskey");
        if (endpoint == null || endpoint.isBlank() || accessKey == null || accessKey.isBlank()) {
            throw new IllegalArgumentException("The ACS connection string needs an endpoint and an accesskey");
        }
        return new AcsConnectionString(URI.create(endpoint), Base64.getDecoder().decode(accessKey));
    }
}
