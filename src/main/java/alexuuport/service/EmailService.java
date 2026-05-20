package alexuuport.service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import alexuuport.util.LoggerUtil;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Properties;

public class EmailService {
    private final String username;
    private final String password;
    private final String fromEmail;
    private final Session session;
    private final LoggerUtil logger;
    private boolean useRealEmail;  // Флаг: использовать реальную отправку или эмуляцию

    public EmailService() {
        this.logger = new LoggerUtil(EmailService.class);
        Properties props = loadConfig();

        // Загружаем конфигурацию
        this.username = props.getProperty("email.username", "");
        this.password = props.getProperty("email.password", "");
        this.fromEmail = props.getProperty("email.from", "test@localhost.com");

        // Решаем, использовать реальную отправку или эмуляцию
        this.useRealEmail = !username.isEmpty() && !password.isEmpty()
                && !"localhost".equals(props.getProperty("mail.smtp.host"));

        if (useRealEmail) {
            // Настройка реального SMTP
            String host = props.getProperty("mail.smtp.host", "smtp.gmail.com");
            String port = props.getProperty("mail.smtp.port", "587");
            String auth = props.getProperty("mail.smtp.auth", "true");
            String starttls = props.getProperty("mail.smtp.starttls.enable", "true");

            Properties mailProps = new Properties();
            mailProps.put("mail.smtp.host", host);
            mailProps.put("mail.smtp.port", port);
            mailProps.put("mail.smtp.auth", auth);
            mailProps.put("mail.smtp.starttls.enable", starttls);

            this.session = Session.getInstance(mailProps, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            logger.info("Email сервис настроен на реальную отправку через {}", host);
        } else {
            this.session = null;
            logger.info("Email сервис работает в режиме ЭМУЛЯЦИИ (письма не отправляются реально)");
        }
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try {
            var inputStream = EmailService.class.getClassLoader()
                    .getResourceAsStream("email.properties");

            if (inputStream != null) {
                props.load(inputStream);
                logger.debug("Конфигурация email загружена из файла");

                // Проверяем, есть ли флаг принудительной эмуляции
                String forceEmulation = props.getProperty("email.force_emulation", "false");
                if ("true".equals(forceEmulation)) {
                    props.setProperty("mail.smtp.host", "localhost");
                    logger.info("Принудительная эмуляция включена");
                }
            } else {
                logger.warn("Файл email.properties не найден, используем эмуляцию");
                setDefaultEmulationProps(props);
            }
        } catch (Exception e) {
            logger.error("Не удалось загрузить конфигурацию", e);
            setDefaultEmulationProps(props);
        }
        return props;
    }

    private void setDefaultEmulationProps(Properties props) {
        props.setProperty("mail.smtp.host", "localhost");
        props.setProperty("mail.smtp.port", "1025");
        props.setProperty("mail.smtp.auth", "false");
        props.setProperty("mail.smtp.starttls.enable", "false");
        props.setProperty("email.from", "emulator@localhost.com");
    }


     // Отправка кода на email (реально или через эмуляцию)

    public void sendCode(String toEmail, String code) {
        logger.info("Попытка отправки email на адрес {}", toEmail);

        if (toEmail == null || toEmail.isEmpty()) {
            logger.error("Email адрес получателя не указан");
            throw new RuntimeException("Email адрес не может быть пустым");
        }

        if (useRealEmail) {
            // Реальная отправка через SMTP
            sendRealEmail(toEmail, code);
        } else {
            // Эмуляция отправки
            emulateEmailDelivery(toEmail, code);
        }
    }


    // Реальная отправка письма через SMTP

    private void sendRealEmail(String toEmail, String code) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Ваш OTP код подтверждения");
            message.setContent(
                    String.format(
                            "<html>" +
                                    "<body style='font-family: Arial, sans-serif;'>" +
                                    "<h2>Ваш код подтверждения</h2>" +
                                    "<p>Код для подтверждения операции: <strong style='font-size: 24px;'>%s</strong></p>" +
                                    "<p>Код действителен в течение 5 минут.</p>" +
                                    "<hr>" +
                                    "<p style='color: gray; font-size: 12px;'>Это автоматическое сообщение, не отвечайте на него.</p>" +
                                    "</body>" +
                                    "</html>",
                            code
                    ),
                    "text/html; charset=utf-8"
            );

            Transport.send(message);
            logger.success("Email реально отправлен на {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Ошибка реальной отправки email: {}", e.getMessage());
            logger.info("Переключение в режим эмуляции для этого письма");
            emulateEmailDelivery(toEmail, code);
        }
    }


     // ЭМУЛЯЦИЯ отправки письма

    private void emulateEmailDelivery(String toEmail, String code) {
        // Формируем красивое эмуляционное сообщение
        String emulationOutput = buildEmulationMessage(toEmail, code);

        // 1. Выводим в консоль (для немедленной отладки)
        System.out.println(emulationOutput);

        // 2. Сохраняем в файл (для истории)
        saveToEmulationFile(emulationOutput);

        // 3. Логируем эмуляцию
        logger.success("ЭМУЛЯЦИЯ: Письмо для {} с кодом {} записано в лог", toEmail, code);

        // 4. Опционально: сохраняем в JSON формате для автоматического тестирования
        saveToJsonFormat(toEmail, code);
    }

    // Создание эмуляционного письма
    private String buildEmulationMessage(String toEmail, String code) {
        String timestamp = LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        );

        return String.format(
                "\n" +
                        "╔════════════════════════════════════════════════════════════════╗\n" +
                        "║                      ЭМУЛЯЦИЯ EMAIL ОТПРАВКИ                   ║\n" +
                        "╠════════════════════════════════════════════════════════════════╣\n" +
                        "║ Время: %-50s ║\n" +
                        "║ Отправитель: %-45s ║\n" +
                        "║ Получатель: %-48s ║\n" +
                        "║ Тема: %-53s ║\n" +
                        "╠════════════════════════════════════════════════════════════════╣\n" +
                        "║                         ТЕЛО ПИСЬМА                              ║\n" +
                        "╠════════════════════════════════════════════════════════════════╣\n" +
                        "║ Ваш код подтверждения: %-36s ║\n" +
                        "║                                                              ║\n" +
                        "║ Код действителен в течение 5 минут.                          ║\n" +
                        "║                                                              ║\n" +
                        "║ Это автоматическое сообщение, не отвечайте на него.          ║\n" +
                        "╚════════════════════════════════════════════════════════════════╝\n",
                timestamp,
                fromEmail,
                toEmail,
                "Ваш OTP код подтверждения",
                code
        );
    }

    /**
     * Сохранение эмуляции в текстовый файл
     */
    private void saveToEmulationFile(String message) {
        try {
            String fileName = "email_emulation.log";
            Files.writeString(
                    Paths.get(fileName),
                    message + "\n",
                    Files.exists(Paths.get(fileName)) ?
                            java.nio.file.StandardOpenOption.APPEND :
                            java.nio.file.StandardOpenOption.CREATE
            );
            logger.debug("Эмуляция сохранена в файл: {}", fileName);
        } catch (Exception e) {
            logger.error("Ошибка сохранения эмуляции в файл", e);
        }
    }

    /**
     * Сохранение в JSON формате (для автоматизации)
     */
    private void saveToJsonFormat(String toEmail, String code) {
        try {
            String json = String.format(
                    "{\"timestamp\":\"%s\",\"to\":\"%s\",\"code\":\"%s\",\"type\":\"email_emulation\"}\n",
                    LocalDateTime.now(),
                    toEmail,
                    code
            );

            Files.writeString(
                    Paths.get("email_emulation.json"),
                    json,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            // Игнорируем ошибки JSON, это не критично
        }
    }

    /**
     * Метод для проверки, работает ли сервис в режиме эмуляции
     */
    public boolean isEmulationMode() {
        return !useRealEmail;
    }
}