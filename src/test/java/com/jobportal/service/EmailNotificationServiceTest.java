package com.jobportal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {
    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    private EmailNotificationService notificationService;

    @BeforeEach
    void setUp() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        notificationService = new EmailNotificationService(mailSenderProvider, "team@jobhire.example");
    }

    @Test
    void applicationSubmissionNotifiesApplicantAndEmployer() {
        notificationService.applicationSubmitted(
                "applicant@example.com", "Asha", "employer@example.com", "Northstar", "Backend Engineer");

        ArgumentCaptor<SimpleMailMessage> messages = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(messages.capture());
        List<SimpleMailMessage> sentMessages = messages.getAllValues();

        assertEquals("applicant@example.com", sentMessages.get(0).getTo()[0]);
        assertEquals("Application received: Backend Engineer", sentMessages.get(0).getSubject());
        assertEquals("employer@example.com", sentMessages.get(1).getTo()[0]);
        assertEquals("New applicant for Backend Engineer", sentMessages.get(1).getSubject());
        assertEquals("team@jobhire.example", sentMessages.get(0).getFrom());
    }

    @Test
    void statusChangeNotifiesApplicant() {
        notificationService.applicationStatusChanged(
                "applicant@example.com", "Asha", "Backend Engineer", "interview");

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());

        assertEquals("applicant@example.com", message.getValue().getTo()[0]);
        assertEquals("Application update: Backend Engineer", message.getValue().getSubject());
    }
}
