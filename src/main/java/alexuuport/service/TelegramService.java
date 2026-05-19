package alexuuport.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class TelegramService {
    private final String botToken;
    private final String chatId;
    private final String telegramApiUrl;

    public TelegramService() {
        Properties props = loadConfig();
        this.botToken = props.getProperty("telegram.bot.token");
        this.chatId = props.getProperty("telegram.chat.id");
        this.telegramApiUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";
    }

    private Properties loadConfig() {
        try {
            Properties props = new Properties();
            props.load(TelegramService.class.getClassLoader().getResourceAsStream("telegram.properties"));
            return props;
        } catch (Exception e) {
            throw new RuntimeException("Не удалось загрузить конфигурацию Telegram", e);
        }
    }

    public void sendCode(String username, String code) {
        String message = String.format("🔐 %s, ваш код подтверждения: %s", username, code);
        String url = String.format("%s?chat_id=%s&text=%s",
                telegramApiUrl,
                chatId,
                urlEncode(message));

        sendTelegramRequest(url);
    }

    private void sendTelegramRequest(String url) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Ошибка Telegram API. Статус: " + response.statusCode());
            } else {
                System.out.println("Telegram сообщение успешно отправлено");
            }
        } catch (Exception e) {
            System.err.println("Ошибка отправки Telegram сообщения: " + e.getMessage());
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
