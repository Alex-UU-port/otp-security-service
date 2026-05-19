package alexuuport.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerUtil {
    private final String className;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public LoggerUtil(Class<?> clazz) {
        this.className = clazz.getSimpleName();
    }

    private String formatMessage(String level, String message, Object... args) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String formattedMessage = String.format(message, args);
        return String.format("[%s] [%s] [%s] %s", timestamp, level, className, formattedMessage);
    }

    public void info(String message, Object... args) {
        System.out.println(formatMessage("INFO", message, args));
    }

    public void debug(String message, Object... args) {
        System.out.println(formatMessage("DEBUG", message, args));
    }

    public void warn(String message, Object... args) {
        System.err.println(formatMessage("WARN", message, args));
    }

    public void error(String message, Object... args) {
        System.err.println(formatMessage("ERROR", message, args));
    }

    public void error(String message, Throwable throwable, Object... args) {
        System.err.println(formatMessage("ERROR", message, args));
        throwable.printStackTrace(System.err);
    }

    public void success(String message, Object... args) {
        System.out.println(formatMessage("SUCCESS", "✅ " + message, args));
    }

    /**
     * Логирование HTTP запроса
     */
    public void logRequest(String method, String path, String clientIp) {
        info("Входящий запрос: {} {} от {}", method, path, clientIp);
    }

    /**
     * Логирование HTTP ответа
     */
    public void logResponse(String method, String path, int statusCode, long durationMs) {
        info("Ответ на запрос: {} {} - статус {} за {} мс", method, path, statusCode, durationMs);
    }

    /**
     * Логирование действия пользователя
     */
    public void logUserAction(String username, String action, String details) {
        info("Действие пользователя {}: {} - {}", username, action, details);
    }

    /**
     * Логирование генерации OTP
     */
    public void logOtpGeneration(String operationId, String channel, String destination) {
        info("Сгенерирован OTP код: операция={}, канал={}, получатель={}", operationId, channel, destination);
    }

    /**
     * Логирование валидации OTP
     */
    public void logOtpValidation(String operationId, boolean success, String username) {
        if (success) {
            success("Успешная валидация OTP кода: операция={}, пользователь={}", operationId, username);
        } else {
            warn("Ошибка валидации OTP кода: операция={}, пользователь={}", operationId, username);
        }
    }

    /**
     * Логирование отправки кода через канал
     */
    public void logChannelDelivery(String channel, String destination, boolean success) {
        if (success) {
            info("Код успешно отправлен через {} на {}", channel, destination);
        } else {
            error("Не удалось отправить код через {} на {}", channel, destination);
        }
    }
}
