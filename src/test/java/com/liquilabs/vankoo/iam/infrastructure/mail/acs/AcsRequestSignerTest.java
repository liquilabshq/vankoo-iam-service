package com.liquilabs.vankoo.iam.infrastructure.mail.acs;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AcsRequestSignerTest {

    private static final URI SEND = URI.create(
            "https://vankoo-acs.unitedstates.communication.azure.com/emails:send?api-version=2023-03-31");
    private static final byte[] BODY = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);
    // Vectores calculados aparte (Python: hashlib + hmac) con la misma clave y cuerpo.
    private static final String BODY_HASH = "AVq9f1zFei3ZS3WQ8ErYCEJzkF7jPsXOvq5iJ2qX+GI=";

    private final AcsRequestSigner signer =
            new AcsRequestSigner("a-test-access-key".getBytes(StandardCharsets.UTF_8));

    @Test
    void signsMethodPathDateHostAndBodyHash() {
        var headers = signer.sign("POST", SEND, BODY, Instant.parse("2026-11-24T20:00:00Z"));

        assertThat(headers).containsEntry("x-ms-date", "Tue, 24 Nov 2026 20:00:00 GMT")
                .containsEntry("x-ms-content-sha256", BODY_HASH)
                .containsEntry("Authorization", "HMAC-SHA256 SignedHeaders=x-ms-date;host;x-ms-content-sha256"
                        + "&Signature=32j+2U9ZR3IAcd8VgUUlCY9t1RF3x/9ENXnyiqbncuo=");
    }

    @Test
    void padsTheDayOfMonthWithTwoDigits() {
        var headers = signer.sign("POST", SEND, BODY, Instant.parse("2026-12-03T09:05:00Z"));

        assertThat(headers.get("x-ms-date")).isEqualTo("Thu, 03 Dec 2026 09:05:00 GMT");
        assertThat(headers.get("Authorization")).endsWith("Signature=SHAfLf02uergvJlK0Qg2vpqC97t7yn/kOf5K0U4fc7k=");
    }
}
