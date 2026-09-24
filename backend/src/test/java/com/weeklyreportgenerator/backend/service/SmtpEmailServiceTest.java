package com.weeklyreportgenerator.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;

import jakarta.mail.internet.MimeMessage;

// GreenMail is a pure-Java in-memory SMTP server -- no Docker needed, unlike the rest of this
// project's Testcontainers-based integration tests. This actually sends real MIME messages over a
// real (loopback) SMTP connection and reads them back, rather than mocking JavaMailSender.
class SmtpEmailServiceTest {

    private GreenMail greenMail;
    private SmtpEmailService emailService;

    @BeforeEach
    void startGreenMail() {
        greenMail = new GreenMail(ServerSetup.SMTP.dynamicPort());
        greenMail.start();

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(greenMail.getSmtp().getPort());

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        emailService = new SmtpEmailService(mailSender, templateEngine);
        ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@weeklyreportgenerator.local");
    }

    @AfterEach
    void stopGreenMail() {
        greenMail.stop();
    }

    @Test
    void sendInvitationEmailDeliversHtmlAndTextWithTheInvitationLink() throws Exception {
        Instant expiresAt = Instant.now().plus(48, ChronoUnit.HOURS);
        emailService.sendInvitationEmail(
                "newhire@example.com", "MANAGER", "Alice Admin",
                "http://localhost:5173/accept-invitation?token=abc123", expiresAt);

        greenMail.waitForIncomingEmail(5000, 1);
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);

        MimeMessage message = messages[0];
        assertThat(message.getSubject()).isEqualTo("You're invited to Weekly Report Generator");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("newhire@example.com");
        assertThat(message.getFrom()[0].toString()).contains("no-reply@weeklyreportgenerator.local");

        String body = GreenMailUtil.getBody(message);
        assertThat(body).contains("http://localhost:5173/accept-invitation?token=abc123");
        assertThat(body).contains("MANAGER");
        assertThat(body).contains("Alice Admin");
    }

    @Test
    void sendPasswordResetEmailDeliversHtmlAndTextWithTheResetLink() throws Exception {
        Instant expiresAt = Instant.now().plus(30, ChronoUnit.MINUTES);
        emailService.sendPasswordResetEmail(
                "member@example.com", "Jordan Member",
                "http://localhost:5173/reset-password?token=xyz789", expiresAt);

        greenMail.waitForIncomingEmail(5000, 1);
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);

        MimeMessage message = messages[0];
        assertThat(message.getSubject()).isEqualTo("Reset your Weekly Report Generator password");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("member@example.com");

        String body = GreenMailUtil.getBody(message);
        assertThat(body).contains("http://localhost:5173/reset-password?token=xyz789");
        assertThat(body).contains("Jordan Member");
    }

    @Test
    void aSendFailureIsSwallowedNotThrown() {
        greenMail.stop(); // no SMTP server listening any more

        // Must not throw -- a slow/unreachable SMTP server is logged, never propagated.
        emailService.sendInvitationEmail(
                "nobody@example.com", "TEAM_MEMBER", "Alice Admin",
                "http://localhost:5173/accept-invitation?token=abc", Instant.now().plusSeconds(3600));
    }
}
