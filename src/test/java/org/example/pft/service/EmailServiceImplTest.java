package org.example.pft.service;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.example.pft.exception.EmailSendException;
import org.example.pft.service.impl.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {
    private static final String TO = "user@example.com";

    @Mock
    JavaMailSender mailSender;

    EmailServiceImpl emailService;

    @BeforeEach
    void setup() {
        emailService = new EmailServiceImpl(mailSender);
    }

    @Test
    void sendReport_shouldCreateReportEmailWithAttachmentAndSubmitMessage() throws Exception {
        MimeMessage message = newMimeMessage();
        byte[] attachment = "%PDF unit test content".getBytes(StandardCharsets.UTF_8);
        when(mailSender.createMimeMessage())
                .thenReturn(message);

        emailService.sendReport(
                TO,
                "Monthly Financial Report",
                "Please find your monthly financial report attached.",
                attachment,
                "2_summary_apr_2024.pdf"
        );

        MimeMessage sentMessage = captureSentMessage();
        sentMessage.saveChanges();

        assertRecipients(sentMessage, TO);
        assertEquals("Monthly Financial Report", sentMessage.getSubject());

        assertEquals(
                "Please find your monthly financial report attached.",
                findTextContent(sentMessage, "text/plain")
        );

        Part attachmentPart = findAttachment(sentMessage, "2_summary_apr_2024.pdf");
        assertNotNull(attachmentPart);
        assertEquals("2_summary_apr_2024.pdf", attachmentPart.getFileName());
        assertEquals(Part.ATTACHMENT, attachmentPart.getDisposition());
        assertArrayEquals(
                attachment,
                attachmentPart.getInputStream().readAllBytes()
        );
        verifyNoMoreInteractions(mailSender);
    }

    @Test
    void sendOtpMail_shouldCreateHtmlOtpEmailAndSubmitMessage() throws Exception {
        MimeMessage message = newMimeMessage();
        when(mailSender.createMimeMessage())
                .thenReturn(message);

        emailService.sendOtpMail(TO, "123456");

        MimeMessage sentMessage = captureSentMessage();
        sentMessage.saveChanges();

        assertRecipients(sentMessage, TO);
        assertEquals("Your verification code", sentMessage.getSubject());

        String html = findTextContent(sentMessage, "text/html");
        assertNotNull(html);
        assertTrue(html.contains("Your verification code"));
        assertTrue(html.contains("123456"));
        assertTrue(html.contains("This code expires in 5 minutes."));
        verifyNoMoreInteractions(mailSender);
    }

    @Test
    void sendReport_whenMessageCompositionFails_shouldThrowEmailSendExceptionWithCause() {
        MessagingException messagingException = new MessagingException("recipient failure");
        when(mailSender.createMimeMessage())
                .thenReturn(new RecipientFailingMimeMessage(messagingException));

        EmailSendException exception = assertThrows(
                EmailSendException.class,
                () -> emailService.sendReport(
                        TO,
                        "Monthly Financial Report",
                        "Please find your monthly financial report attached.",
                        "%PDF".getBytes(StandardCharsets.UTF_8),
                        "report.pdf"
                )
        );

        assertEquals("Failed to send report email", exception.getMessage());
        assertSame(messagingException, exception.getCause());
        verify(mailSender).createMimeMessage();
        verifyNoMoreInteractions(mailSender);
    }

    @Test
    void sendReport_whenMailSenderFails_shouldThrowEmailSendExceptionWithCause() {
        MimeMessage message = newMimeMessage();
        MailSendException mailException = new MailSendException("smtp failure");
        when(mailSender.createMimeMessage())
                .thenReturn(message);
        org.mockito.Mockito.doThrow(mailException)
                .when(mailSender)
                .send(message);

        EmailSendException exception = assertThrows(
                EmailSendException.class,
                () -> emailService.sendReport(
                        TO,
                        "Monthly Financial Report",
                        "Please find your monthly financial report attached.",
                        "%PDF".getBytes(StandardCharsets.UTF_8),
                        "report.pdf"
                )
        );

        assertEquals("Failed to send report email", exception.getMessage());
        assertSame(mailException, exception.getCause());
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(message);
        verifyNoMoreInteractions(mailSender);
    }

    @Test
    void sendOtpMail_whenMessageCompositionFails_shouldThrowEmailSendException() {
        when(mailSender.createMimeMessage())
                .thenReturn(new RecipientFailingMimeMessage(new MessagingException("recipient failure")));

        EmailSendException exception = assertThrows(
                EmailSendException.class,
                () -> emailService.sendOtpMail(TO, "123456")
        );

        assertEquals("Failed to send verification code", exception.getMessage());
        verify(mailSender).createMimeMessage();
        verifyNoMoreInteractions(mailSender);
    }

    @Test
    void sendOtpMail_whenMailSenderFails_shouldPropagateMailException() {
        MimeMessage message = newMimeMessage();
        MailSendException mailException = new MailSendException("smtp failure");
        when(mailSender.createMimeMessage())
                .thenReturn(message);
        org.mockito.Mockito.doThrow(mailException)
                .when(mailSender)
                .send(message);

        MailSendException exception = assertThrows(
                MailSendException.class,
                () -> emailService.sendOtpMail(TO, "123456")
        );

        assertSame(mailException, exception);
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(message);
        verifyNoMoreInteractions(mailSender);
    }

    private MimeMessage captureSentMessage() {
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(messageCaptor.capture());
        return messageCaptor.getValue();
    }

    private static MimeMessage newMimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private static void assertRecipients(MimeMessage message, String expectedRecipient) throws Exception {
        Address[] recipients = message.getRecipients(Message.RecipientType.TO);

        assertEquals(1, recipients.length);
        assertEquals(expectedRecipient, recipients[0].toString());
    }

    private static String findTextContent(Part part, String mimeType) throws Exception {
        if (part.isMimeType(mimeType)) {
            return part.getContent().toString();
        }

        Object content = part.getContent();
        if (content instanceof MimeMultipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String text = findTextContent(multipart.getBodyPart(i), mimeType);
                if (text != null) {
                    return text;
                }
            }
        }

        return null;
    }

    private static Part findAttachment(Part part, String fileName) throws Exception {
        if (fileName.equals(part.getFileName())) {
            return part;
        }

        Object content = part.getContent();
        if (content instanceof MimeMultipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                Part attachment = findAttachment(multipart.getBodyPart(i), fileName);
                if (attachment != null) {
                    return attachment;
                }
            }
        }

        return null;
    }

    private static final class RecipientFailingMimeMessage extends MimeMessage {
        private final MessagingException exception;

        private RecipientFailingMimeMessage(MessagingException exception) {
            super(Session.getInstance(new Properties()));
            this.exception = exception;
        }

        @Override
        public void setRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
            throw exception;
        }
    }
}
