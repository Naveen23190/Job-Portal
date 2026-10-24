package com.jobportal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.notifications.email.enabled", havingValue = "true")
public class EmailNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;

    public EmailNotificationService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.notifications.email.from}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress;
    }

    @Async
    public void applicationSubmitted(
            String applicantEmail, String applicantName, String employerEmail,
            String employerName, String jobTitle) {
        send(applicantEmail, "Application received: " + jobTitle,
                "Hi " + applicantName + ",\n\nYour application for " + jobTitle
                        + " has been submitted successfully. You can follow its progress in your JobHire account.");
        send(employerEmail, "New applicant for " + jobTitle,
                "Hi " + employerName + ",\n\nA candidate has applied for " + jobTitle
                        + ". Sign in to JobHire to review the application.");
    }

    @Async
    public void applicationStatusChanged(
            String applicantEmail, String applicantName, String jobTitle, String status) {
        send(applicantEmail, "Application update: " + jobTitle,
                "Hi " + applicantName + ",\n\nYour application for " + jobTitle
                        + " has been updated to " + status + ". Sign in to JobHire to view the latest details.");
    }

    @Async
    public void applicationWithdrawn(
            String employerEmail, String employerName, String applicantName, String jobTitle) {
        send(employerEmail, "Application withdrawn: " + jobTitle,
                "Hi " + employerName + ",\n\n" + applicantName
                        + " has withdrawn their application for " + jobTitle + ".");
    }

    private void send(String recipient, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            logger.error("Email notification was not sent because no mail sender is configured");
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            logger.error("Email notification could not be sent", exception);
        }
    }
}
