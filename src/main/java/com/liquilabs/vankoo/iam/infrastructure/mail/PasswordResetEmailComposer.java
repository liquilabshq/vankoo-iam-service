package com.liquilabs.vankoo.iam.infrastructure.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Arma el correo de recuperación, sea cual sea el proveedor que lo envíe.
 * <p>
 * El mensaje lleva dos enlaces con el mismo token. El botón principal abre la web,
 * que funciona en cualquier sitio; debajo va el esquema de la app, para quien lea el
 * correo en el móvil. El día que haya un dominio propio con App Links, el segundo
 * sobra y no cambia nada más: el backend seguirá emitiendo un solo token.
 */
@Component
public class PasswordResetEmailComposer {

    private static final String TEMPLATE = "mail/password-reset";
    private static final String SUBJECT = "Recupera tu contraseña de Vankoo";
    private static final String TOKEN_PARAMETER = "token";

    private final ITemplateEngine templateEngine;
    private final String webUrl;
    private final String mobileUrl;

    public PasswordResetEmailComposer(
            ITemplateEngine templateEngine,
            @Value("${mail.password-reset.web-url}") String webUrl,
            @Value("${mail.password-reset.mobile-url}") String mobileUrl
    ) {
        this.templateEngine = templateEngine;
        this.webUrl = webUrl;
        this.mobileUrl = mobileUrl;
    }

    public PasswordResetEmail compose(String token, Date expiresAt) {
        var context = new Context();
        context.setVariable("webLink", linkOf(webUrl, token));
        context.setVariable("mobileLink", linkOf(mobileUrl, token));
        context.setVariable("expirationMinutes", minutesUntil(expiresAt));
        return new PasswordResetEmail(SUBJECT, templateEngine.process(TEMPLATE, context));
    }

    /**
     * Un solo sitio donde se pega el token a una URL, para que la codificación del
     * parámetro esté escrita una vez. Vale igual para https y para vankoo://.
     */
    private String linkOf(String base, String token) {
        return UriComponentsBuilder.fromUriString(base)
                .queryParam(TOKEN_PARAMETER, token)
                .build()
                .toUriString();
    }

    private long minutesUntil(Date expiresAt) {
        var remaining = expiresAt.getTime() - System.currentTimeMillis();
        return Math.max(1, TimeUnit.MILLISECONDS.toMinutes(remaining));
    }

    /** Asunto y cuerpo HTML, listos para cualquier proveedor. */
    public record PasswordResetEmail(String subject, String html) {
    }
}
