package alexuuport.server.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.otp.dao.UserDao;
import com.otp.model.User;
import com.otp.util.JwtUtil;
import com.otp.util.PasswordUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.stream.Collectors;

public class AuthHandler implements HttpHandler {
    private final Connection connection;
    private final Gson gson = new Gson();

    public AuthHandler(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void handle(HttpExchange exchange) throws java.io.IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("/api/auth/register".equals(path) && "POST".equals(method)) {
                handleRegister(exchange);
            } else if ("/api/auth/login".equals(path) && "POST".equals(method)) {
                handleLogin(exchange);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Эндпоинт не найден\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Внутренняя ошибка сервера\"}");
        }
    }

    private void handleRegister(HttpExchange exchange) throws Exception {
        String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining());

        JsonObject json = gson.fromJson(body, JsonObject.class);
        String username = json.get("username").getAsString();
        String password = json.get("password").getAsString();
        String email = json.has("email") ? json.get("email").getAsString() : null;
        String phone = json.has("phone") ? json.get("phone").getAsString() : null;

        UserDao userDao = new UserDao(connection);

        // Проверяем, существует ли пользователь
        if (userDao.findByUsername(username) != null) {
            sendResponse(exchange, 400, "{\"error\":\"Пользователь уже существует\"}");
            return;
        }

        // Определяем роль
        String role;
        if (!userDao.hasAdminExists()) {
            role = "ADMIN";
            System.out.println("Создан первый администратор: " + username);
        } else {
            role = "USER";
        }

        String passwordHash = PasswordUtil.hashPassword(password);
        User user = new User(username, passwordHash, role, email, phone, null);

        if (userDao.createUser(user)) {
            JsonObject response = new JsonObject();
            response.addProperty("message", "Пользователь успешно зарегистрирован");
            response.addProperty("role", role);
            sendResponse(exchange, 201, gson.toJson(response));
            System.out.println("Зарегистрирован пользователь: " + username + " с ролью: " + role);
        } else {
            sendResponse(exchange, 500, "{\"error\":\"Ошибка регистрации\"}");
        }
    }

    private void handleLogin(HttpExchange exchange) throws Exception {
        String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining());

        JsonObject json = gson.fromJson(body, JsonObject.class);
        String username = json.get("username").getAsString();
        String password = json.get("password").getAsString();

        UserDao userDao = new UserDao(connection);
        User user = userDao.findByUsername(username);

        if (user == null || !PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            sendResponse(exchange, 401, "{\"error\":\"Неверные учетные данные\"}");
            return;
        }

        String token = JwtUtil.generateToken(user.getUsername(), user.getRole());
        JsonObject response = new JsonObject();
        response.addProperty("token", token);
        response.addProperty("username", user.getUsername());
        response.addProperty("role", user.getRole());

        sendResponse(exchange, 200, gson.toJson(response));
        System.out.println("Пользователь вошел в систему: " + username);
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