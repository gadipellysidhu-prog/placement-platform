package com.college.placement.shared.notification.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP-backed {@link EmailProvider} (MailHog in dev, a real relay in production).
 *
 * <p>Delivery is gated by {@code notification.email.enabled}; when disabled the
 * message is logged and skipped so environments without SMTP (tests, bare dev)
 * function normally. {@link JavaMailSender} is resolved lazily via
 * {@link ObjectProvider} so the application starts even when Spring Boot did not
 * auto-configure a mail sender.
 */
@Slf4j
@Component
public class SmtpEmailProvider implements EmailProvider {

    private final NotificationEmailProperties properties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public SmtpEmailProvider(NotificationEmailProperties properties,
                             ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.properties = properties;
        this.mailSenderProvider = mailSenderProvider;
    }

    @Override
    public void send(EmailMessage message) {
        if (!properties.isEnabled()) {
            log.info("EMAIL_SKIPPED reason=disabled to={} subject={}", message.to(), message.subject());
            return;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("EMAIL_SKIPPED reason=no_mail_sender to={} subject={}", message.to(), message.subject());
            return;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(properties.getFrom());
            mail.setTo(message.to());
            mail.setSubject(message.subject());
            mail.setText(message.body());
            sender.send(mail);
            log.info("EMAIL_SENT to={} subject={}", message.to(), message.subject());
        } catch (MailException ex) {
            log.error("EMAIL_SEND_FAILED to={} subject={} error={}",
                    message.to(), message.subject(), ex.getMessage());
            throw new EmailDeliveryException("Failed to send email to " + message.to(), ex);
        }
    }

    @Override
    public String providerId() {
        return "smtp";
    }
}
