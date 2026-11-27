package util;

import java.util.Properties;
import jakarta.mail.*;
import jakarta.mail.internet.*;

public class EmailUtil {
    public static void sendEmail(String toEmail, String subject, String body) {
        sendEmail(toEmail, subject, body, null, null);
    }

    public static void sendHtmlAttachment(String toEmail, String subject, String body, String filename, String html) {
        sendEmail(toEmail, subject, body, filename, html);
    }

    private static void sendEmail(String toEmail, String subject, String body, String filename, String html) {
        // Cấu hình email gửi đi
        final PasswordAuthentication authentication = smtpAuthentication();
        if (isBlank(toEmail)) {
            throw new IllegalArgumentException("Recipient email is required");
        }
        if (authentication == null) {
            throw new IllegalStateException(
                    "SMTP is not configured. Set SMTP_USERNAME, SMTP_PASSWORD and optionally SMTP_FROM.");
        }
        final String fromEmail = readSetting("mail.smtp.from", "SMTP_FROM", authentication.getUserName());

        Properties props = new Properties();
        props.put("mail.smtp.host", readSetting("mail.smtp.host", "SMTP_HOST", "smtp.gmail.com"));
        props.put("mail.smtp.port", readSetting("mail.smtp.port", "SMTP_PORT", "587"));
        props.put("mail.smtp.auth", "true"); 
        props.put("mail.smtp.starttls.enable", "true"); 
        if ("smtp.gmail.com".equalsIgnoreCase(props.getProperty("mail.smtp.host"))) {
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.checkserveridentity", "true");
        }
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");

        // Tạo phiên làm việc (Session)
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return authentication;
            }
        });

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(fromEmail));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            msg.setSubject(subject, "UTF-8");
            if (html == null) {
                msg.setText(body, "UTF-8");
            } else {
                MimeBodyPart text = new MimeBodyPart();
                text.setText(body, "UTF-8");
                MimeBodyPart attachment = new MimeBodyPart();
                attachment.setContent(html, "text/html; charset=UTF-8");
                attachment.setFileName(filename);
                attachment.setDisposition(Part.ATTACHMENT);
                Multipart parts = new MimeMultipart("mixed");
                parts.addBodyPart(text);
                parts.addBodyPart(attachment);
                msg.setContent(parts);
            }

            Transport.send(msg);
            System.out.println("Gửi email thành công tới: " + toEmail);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi gửi email: " + e.getMessage());
        }
    }

    public static boolean isConfigured() {
        return smtpAuthentication() != null;
    }

    private static PasswordAuthentication smtpAuthentication() {
        String username = readSetting("mail.smtp.username", "SMTP_USERNAME", "");
        String password = readSetting("mail.smtp.password", "SMTP_PASSWORD", "");
        if (!isBlank(username) && !isBlank(password)) return new PasswordAuthentication(username, password);
        if (isBlank(username) && isBlank(password)
                && "smtp.gmail.com".equalsIgnoreCase(readSetting("mail.smtp.host", "SMTP_HOST", "smtp.gmail.com"))
                && "587".equals(readSetting("mail.smtp.port", "SMTP_PORT", "587"))) {
            return WindowsSmtpCredential.get();
        }
        return null;
    }

    private static String readSetting(String propertyName, String environmentName, String fallback) {
        String value = System.getProperty(propertyName);
        if (isBlank(value)) value = System.getenv(environmentName);
        return isBlank(value) ? fallback : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
