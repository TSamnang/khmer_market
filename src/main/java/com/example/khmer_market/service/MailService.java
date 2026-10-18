package com.example.khmer_market.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    @Value("${market.business.mail-from:no-reply@khmermarket.gov.kh}")
    private String mailFrom;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject("Khmer Marketplace - Password Reset Request / សំណើកំណត់ពាក្យសម្ងាត់ឡើងវិញ");
            message.setText("Dear User,\n\n" +
                    "A password reset request was received for your Khmer Marketplace account.\n" +
                    "To set a new password, click the link below (valid for 30 minutes):\n\n" +
                    resetUrl + "\n\n" +
                    "If you did not request this, please disregard this email. Your password remains unchanged.\n\n" +
                    "សូមគោរព,\n" +
                    "មានសំណើកំណត់ពាក្យសម្ងាត់ឡើងវិញសម្រាប់គណនីផ្សារខ្មែររបស់អ្នក។\n" +
                    "សូមចុចតំណភ្ជាប់ខាងលើដើម្បីកំណត់ពាក្យសម្ងាត់ថ្មី (មានសុពលភាព ៣០ នាទី)។\n\n" +
                    "Khmer Marketplace Team");

            mailSender.send(message);
            log.info("Password reset email sent to recipient");
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
        }
    }
}
