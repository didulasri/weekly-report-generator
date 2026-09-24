package com.weeklyreportgenerator.backend.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Override
    public void sendInvitationEmail(
            String toEmail, String recipientRole, String invitedByName, String invitationLink, Instant expiresAt) {
        Context context = new Context();
        context.setVariable("recipientRole", recipientRole);
        context.setVariable("invitedByName", invitedByName);
        context.setVariable("invitationLink", invitationLink);
        context.setVariable("expiresAtDisplay", EXPIRY_FORMAT.format(expiresAt));

        String plainText = """
                You've been invited to join Weekly Report Generator as a %s.
                Invited by: %s

                Accept your invitation: %s

                This link expires on %s. If you weren't expecting this invitation, you can ignore this email.
                """.formatted(recipientRole, invitedByName, invitationLink, EXPIRY_FORMAT.format(expiresAt));

        send(toEmail, "You're invited to Weekly Report Generator", "email/invitation", context, plainText);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String recipientName, String resetLink, Instant expiresAt) {
        Context context = new Context();
        context.setVariable("recipientName", recipientName);
        context.setVariable("resetLink", resetLink);
        context.setVariable("expiresAtDisplay", EXPIRY_FORMAT.format(expiresAt));

        String plainText = """
                Hi %s,

                A password reset was requested for your Weekly Report Generator account.

                Reset your password: %s

                This link expires on %s. If you didn't request this, you can safely ignore this email --
                your password will not be changed.
                """.formatted(recipientName, resetLink, EXPIRY_FORMAT.format(expiresAt));

        send(toEmail, "Reset your Weekly Report Generator password", "email/password-reset", context, plainText);
    }

    // A slow or unreachable SMTP server must never surface as a failure of the request that
    // triggered it -- by the time this runs (AFTER_COMMIT, @Async) the triggering request has
    // usually already returned, so there is nothing left to fail; this just logs and moves on.
    private void send(String toEmail, String subject, String templateName, Context context, String plainText) {
        try {
            String html = templateEngine.process(templateName, context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom(fromAddress);
            helper.setSubject(subject);
            helper.setText(plainText, html);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send '{}' email to {}: {}", templateName, toEmail, e.getMessage());
        }
    }
}
