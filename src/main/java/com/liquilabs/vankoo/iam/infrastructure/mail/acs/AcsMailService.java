package com.liquilabs.vankoo.iam.infrastructure.mail.acs;

import com.liquilabs.vankoo.iam.application.internal.outboundservices.mail.MailService;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.Email;
import com.liquilabs.vankoo.iam.infrastructure.mail.PasswordResetEmailComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Envía el correo de recuperación con la API REST de Azure Communication Services
 * ({@code mail.provider=acs}).
 * <p>
 * Se autentica con la clave de la cadena de conexión, sin SDK y sin app de Entra: el
 * SMTP de ACS exige registrar una app, y el tenant de la universidad no lo permite.
 * ACS responde 202 y entrega el correo en segundo plano.
 */
@Service
@ConditionalOnProperty(name = "mail.provider", havingValue = "acs")
public class AcsMailService implements MailService {

    static final String API_VERSION = "2023-03-31";
    private static final Logger LOGGER = LoggerFactory.getLogger(AcsMailService.class);
    private static final Pattern ADDRESS_IN_BRACKETS = Pattern.compile("<([^>]+)>");

    private final PasswordResetEmailComposer composer;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Clock clock;
    private final URI sendUri;
    private final AcsRequestSigner signer;
    private final String senderAddress;

    @Autowired
    public AcsMailService(
            PasswordResetEmailComposer composer,
            ObjectMapper objectMapper,
            @Value("${mail.acs.connection-string}") String connectionString,
            @Value("${mail.from}") String from
    ) {
        this(composer, objectMapper, AcsConnectionString.parse(connectionString), from,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), Clock.systemUTC());
    }

    AcsMailService(
            PasswordResetEmailComposer composer,
            ObjectMapper objectMapper,
            AcsConnectionString connection,
            String from,
            HttpClient httpClient,
            Clock clock
    ) {
        this.composer = composer;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.clock = clock;
        this.sendUri = connection.endpoint().resolve("/emails:send?api-version=" + API_VERSION);
        this.signer = new AcsRequestSigner(connection.accessKey());
        // ACS solo acepta la dirección: el nombre visible lo pone el remitente del dominio.
        var matcher = ADDRESS_IN_BRACKETS.matcher(from);
        this.senderAddress = matcher.find() ? matcher.group(1) : from.trim();
    }

    @Override
    public void sendPasswordResetEmail(Email recipient, String token, Date expiresAt) {
        var email = composer.compose(token, expiresAt);
        byte[] body = objectMapper.writeValueAsBytes(Map.of(
                "senderAddress", senderAddress,
                "recipients", Map.of("to", List.of(Map.of("address", recipient.email()))),
                "content", Map.of("subject", email.subject(), "html", email.html())
        ));

        var request = HttpRequest.newBuilder(sendUri)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        signer.sign("POST", sendUri, body, clock.instant()).forEach(request::header);

        HttpResponse<String> response;
        try {
            response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new IllegalStateException("Azure Communication Services did not answer", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while sending the email", exception);
        }
        if (response.statusCode() != 202) {
            String detail = response.body() == null ? "" : response.body();
            throw new IllegalStateException("Azure Communication Services answered HTTP " + response.statusCode()
                    + " " + detail.substring(0, Math.min(300, detail.length())));
        }
        // Sin el token y sin la dirección completa: esto es un log.
        LOGGER.info("Password reset email accepted by Azure Communication Services");
    }
}
