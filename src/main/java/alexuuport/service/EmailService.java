package alexuuport.service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {
    private final String username;
    private final String password;
    private final String fromEmail;
    private final Session session;

    public EmailService() {
        Properties props = loadConfig();
        this.username = props.getProperty("email.username");
        this.password = props.getProperty("email.password");
        this.fromEmail = props.getProperty("email.from");

        Properties mailProps = new Properties();
        mailProps.put("mail.smtp.host", props.getProperty("mail.smtp.host"));
        mailProps.put("mail.smtp.port", props.getProperty("mail.smtp.port"));
        mailProps.put("mail.smtp.auth", props.getProperty("mail.smtp.auth"));
        mailProps.put("mail.smtp.starttls.enable", props.getProperty("mail.smtp.starttls.enable"));

        this.session = Session.getInstance(mailProps, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    private Properties loadConfig() {
        try {
            Properties props = new Properties();
            props.load(EmailService.class.getClassLoader().getResourceAsStream("email.properties"));
            return props;
        } catch (Exception e) {
            throw new RuntimeException("Не удалось загрузить конфигурацию email", e);
        }
    }

    public void sendCode(String toEmail, String code) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Ваш OTP код подтверждения");
            message.setText("Ваш код подтверждения: " + code + "\n\nКод действителен в течение 5 минут.");

            Transport.send(message);
            System.out.println("Email успешно отправлен на " + toEmail);
        } catch (MessagingException e) {
            throw new RuntimeException("Не удалось отправить email", e);
        }
    }
}