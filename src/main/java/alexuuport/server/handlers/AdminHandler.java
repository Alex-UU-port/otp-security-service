package alexuuport.server.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.otp.dao.OtpConfigDao;
import com.otp.dao.UserDao;
import com.otp.model.OtpConfig;
import com.otp.model.User;
import com.otp.util.JwtUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

public class AdminHandler implements HttpHandler {
    private final Connection connection;
    private final Gson gson = new Gson();

    public AdminHandler(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void handle(HttpExchange exchange) throws java.io.IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // Проверяем авторизацию и права администратора
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendResponse(exchange, 401, "{\"error\":\"Требуется авторизация\"}");
            return;
        }

        String token = authHeader.substring(7);
        String role = JwtUtil.getRoleFromToken(token);

        if (role == null || !"ADMIN".equals(role)) {
            sendResponse(exchange, 403, "{\"error\":\"Доступ запрещен. Требуются права администратора\"}");
            return;
        }

        try {
            if ("/api/admin/config".equals(path) && "PUT".equals(method)) {
                handleUpdateConfig(exchange);
            } else if ("/api/admin/users".equals(path) && "GET".equals(method)) {
                handleGetUsers(exchange);
            } else if (path.startsWith("/api/admin/users/") && "DELETE".equals(method)) {
                handleDeleteUser(exchange, path);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Эндпоинт не найден\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Внутренняя ошибка сервера\"}");
        }
    }

    private void handleUpdateConfig(HttpExchange exchange) throws Exception {
        String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining());

        JsonObject json = gson.fromJson(body, JsonObject.class);
        OtpConfigDao configDao = new OtpConfigDao(connection);
        OtpConfig config = configDao.getConfig();

        if (json.has("codeLength")) {
            config.setCodeLength(json.get("codeLength").getAsInt());
        }
        if (json.has("ttlSeconds")) {
            config.setTtlSeconds(json.get("ttlSeconds").getAsInt());
        }

        configDao.updateConfig(config);

        JsonObject response = new JsonObject();
        response.addProperty("message", "Конфигурация OTP обновлена");
        response.addProperty("codeLength", config.getCodeLength());
        response.addProperty("ttlSeconds", config.getTtlSeconds());

        sendResponse(exchange, 200, gson.toJson(response));
        System.out.println("Администратор обновил конфигурацию OTP");
    }

    private void handleGetUsers(HttpExchange exchange) throws Exception {
        UserDao userDao = new UserDao(connection);
        List<User> users = userDao.getAllNonAdminUsers();

        String json = gson.toJson(users);
        sendResponse(exchange, 200, json);
        System.out.println("Администратор запросил список пользователей");
    }

    private void handleDeleteUser(HttpExchange exchange, String path) throws Exception {
        String[] parts = path.split("/");
        int userId = Integer.parseInt(parts[parts.length - 1]);

        UserDao userDao = new UserDao(connection);
        if (userDao.deleteUser(userId)) {
            JsonObject response = new JsonObject();
            response.addProperty("message", "Пользователь успешно удален");
            sendResponse(exchange, 200, gson.toJson(response));
            System.out.println("Администратор удалил пользователя с ID: " + userId);
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Пользователь не найден\"}");
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws java.io.IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}