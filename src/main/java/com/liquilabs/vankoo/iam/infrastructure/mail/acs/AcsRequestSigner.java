package com.liquilabs.vankoo.iam.infrastructure.mail.acs;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Firma una petición a la API REST de Azure Communication Services con HMAC-SHA256:
 * método, ruta con query, fecha, host y hash del cuerpo.
 *
 * @see <a href="https://learn.microsoft.com/azure/communication-services/tutorials/hmac-header-tutorial">HMAC en ACS</a>
 */
public class AcsRequestSigner {

    // IMF-fixdate (RFC 9110): día con dos cifras, en inglés y en GMT. RFC_1123_DATE_TIME
    // de Java no rellena el día con un cero.
    private static final DateTimeFormatter HTTP_DATE =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);

    private final byte[] accessKey;

    public AcsRequestSigner(byte[] accessKey) {
        this.accessKey = accessKey.clone();
    }

    public Map<String, String> sign(String method, URI uri, byte[] body, Instant now) {
        String date = HTTP_DATE.format(now.atOffset(ZoneOffset.UTC));
        String contentHash = base64(sha256(body));
        String pathAndQuery = uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        String stringToSign = method + "\n" + pathAndQuery + "\n" + date + ";" + uri.getAuthority() + ";" + contentHash;

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-ms-date", date);
        headers.put("x-ms-content-sha256", contentHash);
        headers.put("Authorization", "HMAC-SHA256 SignedHeaders=x-ms-date;host;x-ms-content-sha256&Signature="
                + base64(hmac(stringToSign.getBytes(StandardCharsets.UTF_8))));
        return headers;
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private byte[] hmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(accessKey, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String base64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
}
