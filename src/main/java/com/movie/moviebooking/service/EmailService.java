package com.movie.moviebooking.service;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTicketEmail(String to, String subject, String htmlBody, byte[] pdfBytes, String pdfFilename) {
        log.info("[Email Service] Preparing to send email.");
        log.info("[Email Service] Recipient Email: {}", to);
        log.info("[Email Service] Subject: {}", subject);
        log.info("[Email Service] Attachment Filename: {}", pdfFilename);
        log.info("[Email Service] Attachment Size: {} bytes", pdfBytes != null ? pdfBytes.length : 0);

        try {
            log.info("[Email Service] Initializing MimeMessage...");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (pdfBytes != null) {
                helper.addAttachment(pdfFilename, new ByteArrayResource(pdfBytes));
            }
            log.info("[Email Service] Attempting JavaMailSender.send() execution...");
            mailSender.send(message);
            log.info("[Email Service] JavaMailSender.send() execution completed successfully!");
        } catch (Exception e) {
            log.error("[Email Service] CRITICAL ERROR: JavaMailSender execution failed!", e);
            e.printStackTrace();
        }
    }
}
