package com.liquilabs.vankoo.iam.infrastructure.mail.acs;

import com.liquilabs.vankoo.iam.domain.model.valueobjects.Email;
import com.liquilabs.vankoo.iam.infrastructure.mail.PasswordResetEmailComposer;
import com.liquilabs.vankoo.iam.infrastructure.mail.PasswordResetEmailComposer.PasswordResetEmail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.concurrent.Flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AcsMailServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-24T20:00:00Z");
    private static final Date EXPIRES_AT = Date.from(NOW.plusSeconds(1800));

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final PasswordResetEmailComposer composer = mock(PasswordResetEmailComposer.class);
    private final HttpClient httpClient = mock(HttpClient.class);
    @SuppressWarnings("unchecked")
    private final HttpResponse<String> response = mock(HttpResponse.class);
    private AcsMailService service;

    @BeforeEach
    void setUp() throws Exception {
        when(composer.compose("token-1", EXPIRES_AT))
                .thenReturn(new PasswordResetEmail("Recupera tu contraseña de Vankoo", "<p>hola</p>"));
        when(httpClient.<String>send(any(), any())).thenReturn(response);
        service = new AcsMailService(composer, objectMapper,
                AcsConnectionString.parse("endpoint=https://vankoo-acs.unitedstates.communication.azure.com/;"
                        + "accesskey=YS10ZXN0LWFjY2Vzcy1rZXk="),
                "Vankoo <DoNotReply@abc.azurecomm.net>", httpClient, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void postsTheSignedEmailToTheAcsApi() throws Exception {
        when(response.statusCode()).thenReturn(202);

        service.sendPasswordResetEmail(new Email("ana@vankoo.test"), "token-1", EXPIRES_AT);

        var captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        HttpRequest request = captor.getValue();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.uri()).hasToString(
                "https://vankoo-acs.unitedstates.communication.azure.com/emails:send?api-version=2023-03-31");
        assertThat(request.headers().firstValue("x-ms-date")).contains("Tue, 24 Nov 2026 20:00:00 GMT");
        assertThat(request.headers().firstValue("Authorization")).hasValueSatisfying(
                value -> assertThat(value).startsWith("HMAC-SHA256 SignedHeaders=x-ms-date;host;x-ms-content-sha256&Signature="));

        JsonNode body = objectMapper.readTree(bodyOf(request));
        assertThat(body.get("senderAddress").asString()).isEqualTo("DoNotReply@abc.azurecomm.net");
        assertThat(body.at("/recipients/to/0/address").asString()).isEqualTo("ana@vankoo.test");
        assertThat(body.at("/content/subject").asString()).isEqualTo("Recupera tu contraseña de Vankoo");
        assertThat(body.at("/content/html").asString()).isEqualTo("<p>hola</p>");
    }

    @Test
    void failsWhenAcsDoesNotAcceptTheEmail() {
        when(response.statusCode()).thenReturn(401);
        when(response.body()).thenReturn("{\"error\":{\"code\":\"Denied\"}}");

        assertThatThrownBy(() -> service.sendPasswordResetEmail(new Email("ana@vankoo.test"), "token-1", EXPIRES_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP 401");
    }

    @Test
    void failsWhenAcsDoesNotAnswer() throws Exception {
        when(httpClient.<String>send(any(), any())).thenThrow(new IOException("timeout"));

        assertThatThrownBy(() -> service.sendPasswordResetEmail(new Email("ana@vankoo.test"), "token-1", EXPIRES_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    /** Lee el cuerpo de un BodyPublisher, que solo se puede consumir suscribiéndose. */
    private static String bodyOf(HttpRequest request) {
        var out = new StringBuilder();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            @Override public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            @Override public void onNext(ByteBuffer item) { out.append(StandardCharsets.UTF_8.decode(item)); }
            @Override public void onError(Throwable throwable) { throw new AssertionError(throwable); }
            @Override public void onComplete() { }
        });
        return out.toString();
    }
}
