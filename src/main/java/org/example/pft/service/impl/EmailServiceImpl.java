package org.example.pft.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.example.pft.exception.EmailSendException;
import org.example.pft.service.EmailService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@AllArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;

    @Override
    public void sendReport(String to, String subject, String body, byte[] attachment, String fileName) {
        MimeMessage message = mailSender.createMimeMessage();

        try{
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);

            helper.addAttachment(fileName,new ByteArrayResource(attachment));

            mailSender.send(message);
        } catch (MessagingException | MailException ex) {
            throw new EmailSendException("Failed to send report email", ex);
        }
    }

    @Override
    public void sendOtpMail(String to, String otpCode) {
        MimeMessage message = mailSender.createMimeMessage();

        try{
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setSubject(
                    "Your verification code"
            );

            helper.setText(
                    """
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"
                           style="background-color:#f4f6f8;padding:32px 0;font-family:Arial,sans-serif;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="420" cellspacing="0" cellpadding="0"
                                       style="background-color:#ffffff;border-radius:8px;padding:28px;border:1px solid #e5e7eb;">
                                    <tr>
                                        <td align="center" style="font-size:18px;font-weight:600;color:#111827;padding-bottom:18px;">
                                            Your verification code
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center">
                                            <div style="display:inline-block;background-color:#f9fafb;border:1px solid #d1d5db;
                                                        border-radius:8px;padding:16px 32px;font-size:32px;font-weight:700;
                                                        letter-spacing:6px;color:#111827;">
                                                %s
                                            </div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center" style="font-size:14px;color:#6b7280;padding-top:18px;">
                                            This code expires in 5 minutes.
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                    """.formatted(otpCode),
                    true
            );

            mailSender.send(message);


        }catch (MessagingException e){
            throw new EmailSendException("Failed to send verification code");
        }
    }
}
