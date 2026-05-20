package alexuuport.service;

import alexuuport.util.LoggerUtil;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Properties;

public class TelegramService {
    private final String botToken;
    private final String chatId;
    private final String telegramApiUrl;
    private final boolean useRealTelegram;
    private final LoggerUtil logger;

    public TelegramService() {
        this.logger = new LoggerUtil(TelegramService.class);
        Properties props = loadConfig();

        this.botToken = props.getProperty("telegram.bot.token", "");
        this.chatId = props.getProperty("telegram.chat.id", "");

        // Проверяем, нужно ли использовать реальный Telegram
        String emulationMode = props.getProperty("telegram.emulation.mode", "true");
        this.useRealTelegram = !"true".equals(emulationMode)
                && !botToken.isEmpty()
                && !chatId.isEmpty();

        if (useRealTelegram) {
            this.telegramApiUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            logger.info("Telegram сервис настроен на РЕАЛЬНУЮ отправку");
        } else {
            this.telegramApiUrl = null;
            logger.info("Telegram сервис работает в режиме ЭМУЛЯЦИИ");
        }
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try {
            var inputStream = TelegramService.class.getClassLoader()
                    .getResourceAsStream("telegram.properties");

            if (inputStream != null) {
                props.load(inputStream);
                logger.debug("Конфигурация Telegram загружена");
            } else {
                logger.warn("Файл telegram.properties не найден, используем эмуляцию");
                props.setProperty("telegram.emulation.mode", "true");
            }
        } catch (Exception e) {
            logger.error("Не удалось загрузить конфигурацию Telegram", e);
            props.setProperty("telegram.emulation.mode", "true");
        }
        return props;
    }


    // Отправка кода через Telegram (реально или через эмуляцию)

    public void sendCode(String username, String code) {
        String message = String.format(" %s, ваш код подтверждения: %s", username, code);

        if (useRealTelegram) {
            sendRealTelegramMessage(message);
        } else {
            emulateTelegramMessage(username, code);
        }
    }


    // Реальная отправка через Telegram API

    private void sendRealTelegramMessage(String message) {
        logger.info("Отправка реального Telegram сообщения");

        String url = String.format("%s?chat_id=%s&text=%s",
                telegramApiUrl,
                chatId,
                urlEncode(message));

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                logger.success("Telegram сообщение успешно отправлено");

                // Проверяем ответ от Telegram
                if (response.body().contains("\"ok\":true")) {
                    logger.debug("Telegram API подтвердил успешную отправку");
                } else {
                    logger.warn("Telegram API вернул ошибку: {}", response.body());
                }
            } else {
                logger.error("Ошибка Telegram API. Статус: {}", response.statusCode());
                emulateTelegramMessage(null, null);
            }
        } catch (Exception e) {
            logger.error("Ошибка отправки Telegram сообщения: {}", e.getMessage());
            logger.info("Переключение в режим эмуляции");
            emulateTelegramMessage(null, null);
        }
    }


     // Эмуляция отправки Telegram сообщения

    private void emulateTelegramMessage(String username, String code) {
        String emulationOutput = buildEmulationMessage(username, code);

        // Выводим в консоль
        System.out.println(emulationOutput);

        // Сохраняем в файл
        saveToEmulationFile(emulationOutput);

        // Сохраняем в JSON формате
        saveToJsonFormat(username, code);

        logger.success("ЭМУЛЯЦИЯ: Telegram сообщение записано в лог");
    }

    // Создание красивого эмуляционного сообщения

        private String buildEmulationMessage(String username, String code) {
        String timestamp = LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        );

        if (username == null) {
            return String.format(
                    "\n" +
                            "╔════════════════════════════════════════════════════════════════╗\n" +
                            "║                       ЭМУЛЯЦИЯ TELEGRAM ОТПРАВКИ               ║\n" +
                            "╠════════════════════════════════════════════════════════════════╣\n" +
                            "║ Время: %-50s ║\n" +
                            "║ Статус: %-52s ║\n" +
                            "║ Сообщение: Эмуляция активирована из-за ошибки                   ║\n" +
                            "╚════════════════════════════════════════════════════════════════╝\n",
                    timestamp,
                    "ОШИБКА РЕАЛЬНОЙ ОТПРАВКИ"
            );
        }

        return String.format(
                "\n" +
                        "╔════════════════════════════════════════════════════════════════╗\n" +
                        "║                       ЭМУЛЯЦИЯ TELEGRAM ОТПРАВКИ               ║\n" +
                        "╠════════════════════════════════════════════════════════════════╣\n" +
                        "║ Время: %-50s ║\n" +
                        "║ Получатель: %-48s ║\n" +
                        "║ Chat ID: %-51s ║\n" +
                        "╠════════════════════════════════════════════════════════════════╣\n" +
                        "║                        ТЕКСТ СООБЩЕНИЯ                           ║\n" +
                        "╠════════════════════════════════════════════════════════════════╣\n" +
                        "║    %s, ваш код подтверждения: %-28s ║\n" +
                        "╚════════════════════════════════════════════════════════════════╝\n",
                timestamp,
                username,
                chatId != null && !chatId.isEmpty() ? chatId : "Не указан",
                username,
                code
        );
    }

    // Сохранение эмуляции в текстовый файл

    private void saveToEmulationFile(String message) {
        try {
            String fileName = "telegram_emulation.log";
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


    // Сохранение в JSON формате для автоматизации

    private void saveToJsonFormat(String username, String code) {
        if (username == null) return;

        try {
            String json = String.format(
                    "{\"timestamp\":\"%s\",\"username\":\"%s\",\"code\":\"%s\",\"type\":\"telegram_emulation\"}\n",
                    LocalDateTime.now(),
                    username,
                    code
            );

            Files.writeString(
                    Paths.get("telegram_emulation.json"),
                    json,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            // Игнорируем ошибки JSON
        }
    }


    // Проверка, работает ли сервис в режиме эмуляции

    public boolean isEmulationMode() {
        return !useRealTelegram;
    }


    // Проверка подключения к Telegram API

    public boolean checkTelegramConnection() {
        if (!useRealTelegram) {
            logger.warn("Telegram в режиме эмуляции, проверка подключения не требуется");
            return false;
        }

        try {
            String url = String.format("https://api.telegram.org/bot%s/getMe", botToken);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body().contains("\"ok\":true")) {
                logger.success("Telegram бот доступен");
                return true;
            } else {
                logger.error("Telegram бот недоступен");
                return false;
            }
        } catch (Exception e) {
            logger.error("Ошибка проверки Telegram бота: {}", e.getMessage());
            return false;
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}