package alexuuport.server.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.otp.dao.OtpCodeDao;
import com.otp.dao.UserDao;
import com.otp.model.OtpCode;
import com.otp.model.User;
import com.otp.service.*;
import com.otp.util.JwtUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.stream.Collectors;

public class UserHandler implements HttpHandler {
    private final Connection connection;
    private final Gson gson = new Gson();

    public UserHandler(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void handle(HttpExchange exchange) throws java.io.IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // Проверяем авторизацию
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendResponse(exchange, 401, "{\"error\":\"Требуется авторизация\"}");
            return;
        }

        String token = authHeader.substring(7);
        String username = JwtUtil.getUsernameFromToken(token);

        if (username == null) {
            sendResponse(exchange, 401, "{\"error\":\"Недействительный токен\"}");
            return;
        }

        try {
            UserDao userDao = new UserDao(connection);
            User user = userDao.findByUsername(username);

            if (user == null) {
                sendResponse(exchange, 401, "{\"error\":\"Пользователь не найден\"}");
                return;
            }

            if ("/api/user/generate".equals(path) && "POST".equals(method)) {
                handleGenerateOtp(exchange, user);
            } else if ("/api/user/validate".equals(path) && "POST".equals(method)) {
                handleValidateOtp(exchange);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Эндпоинт не найден\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Внутренняя ошибка сервера\"}");
        }
    }

    private void handleGenerateOtp(HttpExchange exchange, User user) throws Exception {
        String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining());

        JsonObject json = gson.fromJson(body, JsonObject.class);
        String channel = json.get("channel").getAsString();
        String destination = json.get("destination").getAsString();

        // Создаем сервисы
        OtpCodeDao otpCodeDao = new OtpCodeDao(connection);
        com.otp.dao.OtpConfigDao otpConfigDao = new com.otp.dao.OtpConfigDao(connection);
        OtpService otpService = new OtpService(otpCodeDao, otpConfigDao);

        // Генерируем OTP код
        OtpCode otpCode = otpService.createOtpCode(user.getId(), channel, destination);

        // Отправляем код через выбранный канал
        switch (channel) {
            case "email":
                EmailService emailService = new EmailService();
                emailService.sendCode(destination, otpCode.getCode());
                break;
            case "telegram":
                TelegramService telegramService = new TelegramService();
                telegramService.sendCode(user.getUsername(), otpCode.getCode());
                break;
            case "sms":
                SmsService smsService = new SmsService();
                smsService.sendCode(destination, otpCode.getCode());
                break;
            case "file":
                FileService fileService = new FileService();
                fileService.saveCodeToFile(otpCode.getOperationId(), otpCode.getCode(), destination, channel);
                break;
            default:
                sendResponse(exchange, 400, "{\"error\":\"Неизвестный канал\"}");
                return;
        }

        JsonObject response = new JsonObject();
        response.addProperty("operationId", otpCode.getOperationId());
        response.addProperty("message", "Код подтверждения отправлен через " + channel);
        response.addProperty("expiresIn", "Код действителен 5 минут");

        sendResponse(exchange, 200, gson.toJson(response));
        System.out.println("Сгенерирован OTP код для пользователя: " + user.getUsername() + " через канал: " + channel);
    }

    private void handleValidateOtp(HttpExchange exchange) throws Exception {
        String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining());

        JsonObject json = gson.fromJson(body, JsonObject.class);
        String operationId = json.get("operationId").getAsString();
        String code = json.get("code").getAsString();

        OtpCodeDao otpCodeDao = new OtpCodeDao(connection);
        com.otp.dao.OtpConfigDao otpConfigDao = new com.otp.dao.OtpConfigDao(connection);
        OtpService otpService = new OtpService(otpCodeDao, otpConfigDao);

        boolean isValid = otpService.validateCode(operationId, code);

        JsonObject response = new JsonObject();
        if (isValid) {
            response.addProperty("success", true);
            response.addProperty("message", "Код подтвержден успешно");
            System.out.println("Успешная валидация OTP кода: " + operationId);
        } else {
            response.addProperty("success", false);
            response.addProperty("message", "Неверный или просроченный код");
            System.out.println("Ошибка валидации OTP кода: " + operationId);
        }

        sendResponse(exchange, 200, gson.toJson(response));
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws java.io.IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}