package com.liquilabs.vankoo.iam.infrastructure.mail;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class PasswordResetEmailComposerTest {

    @Test
    void putsTheTokenInBothLinksAndTheMinutesLeft() {
        ITemplateEngine templateEngine = mock(ITemplateEngine.class);
        when(templateEngine.process(eq("mail/password-reset"), any(Context.class))).thenReturn("<html/>");
        var composer = new PasswordResetEmailComposer(templateEngine,
                "https://vankoo.test/iam/reset", "vankoo://reset-password");

        var email = composer.compose("tok-1", new Date(System.currentTimeMillis() + 30 * 60_000 + 5_000));

        var captor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("mail/password-reset"), captor.capture());
        Context context = captor.getValue();
        assertThat(context.getVariable("webLink")).isEqualTo("https://vankoo.test/iam/reset?token=tok-1");
        assertThat(context.getVariable("mobileLink")).isEqualTo("vankoo://reset-password?token=tok-1");
        assertThat(context.getVariable("expirationMinutes")).isEqualTo(30L);
        assertThat(email.subject()).isEqualTo("Recupera tu contraseña de Vankoo");
        assertThat(email.html()).isEqualTo("<html/>");
    }
}
