package com.liquilabs.vankoo.iam.infrastructure.mail.smtp.services;

import com.liquilabs.vankoo.iam.domain.model.valueobjects.Email;
import com.liquilabs.vankoo.iam.infrastructure.mail.PasswordResetEmailComposer;
import com.liquilabs.vankoo.iam.infrastructure.mail.smtp.SmtpMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Envía el correo de recuperación por SMTP (Mailpit en local). Es el proveedor por
 * defecto; con {@code mail.provider=acs} lo reemplaza {@code AcsMailService}.
 */
@Service
@ConditionalOnProperty(name = "mail.provider", havingValue = "smtp", matchIfMissing = true)
public class MailServiceImpl implements SmtpMailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final PasswordResetEmailComposer composer;
    private final String from;

    public MailServiceImpl(
            JavaMailSender mailSender,
            PasswordResetEmailComposer composer,
            @Value("${mail.from}") String from
    ) {
        this.mailSender = mailSender;
        this.composer = composer;
        this.from = from;
    }

    @Override
    public void sendPasswordResetEmail(Email recipient, String token, Date expiresAt) {
        var email = composer.compose(token, expiresAt);

        // Un MimeMessagePreparator y no un MimeMessage montado a mano: su lambda puede
        // lanzar excepciones comprobadas, así que arma el mensaje sin un try/catch que
        // no sabría qué hacer con el fallo.
        MimeMessagePreparator preparator = mimeMessage -> {
            var helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(recipient.email());
            helper.setSubject(email.subject());
            helper.setText(email.html(), true);
        };
        mailSender.send(preparator);
        // Sin el token y sin la dirección completa: esto es un log, y el correo ya
        // llegó a quien tenía que llegar.
        LOGGER.info("Password reset email sent");
    }
}
