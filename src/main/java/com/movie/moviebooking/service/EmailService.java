package com.movie.moviebooking.service;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTicketEmail(String to, String subject, String htmlBody, byte[] pdfBytes, String pdfFilename) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (pdfBytes != null) {
                helper.addAttachment(pdfFilename, new ByteArrayResource(pdfBytes));
            }
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send ticket email: " + e.getMessage());
        }
    }
}
