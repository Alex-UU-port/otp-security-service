package alexuuport.server;

import com.otp.scheduler.OtpExpiryScheduler;
import com.otp.server.handlers.AdminHandler;
import com.otp.server.handlers.AuthHandler;
import com.otp.server.handlers.UserHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;

public class HttpServer {
    private static final int PORT = 8080;
    private static Connection connection;
    private static OtpExpiryScheduler scheduler;

    public static void main(String[] args) throws Exception {
        // Загружаем драйвер PostgreSQL
        Class.forName("org.postgresql.Driver");

        // Подключаемся к базе данных
        String url = "jdbc:postgresql://localhost:5432/otp_service";
        String user = "postgres";
        String password = "postgres";

        connection = DriverManager.getConnection(url, user, password);
        System.out.println("Подключение к базе данных установлено");

        // Создаем HTTP сервер
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Регистрируем обработчики API
        AuthHandler authHandler = new AuthHandler(connection);
        AdminHandler adminHandler = new AdminHandler(connection);
        UserHandler userHandler = new UserHandler(connection);

        server.createContext("/api/auth/", authHandler);
        server.createContext("/api/admin/", adminHandler);
        server.createContext("/api/user/", userHandler);

        // Обработчик статических файлов (фронтенд)
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            String filePath = "src/main/webapp" + path;
            File file = new File(filePath);

            if (file.exists() && !file.isDirectory()) {
                String mimeType = "text/html";
                if (path.endsWith(".css")) mimeType = "text/css";
                if (path.endsWith(".js")) mimeType = "application/javascript";

                exchange.getResponseHeaders().set("Content-Type", mimeType + "; charset=UTF-8");
                exchange.sendResponseHeaders(200, file.length());
                try (var os = exchange.getResponseBody()) {
                    Files.copy(file.toPath(), os);
                }
            } else {
                String response = "Страница не найдена";
                exchange.sendResponseHeaders(404, response.length());
                try (var os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        });

        // Запускаем планировщик для проверки просроченных кодов
        com.otp.dao.OtpCodeDao otpCodeDao = new com.otp.dao.OtpCodeDao(connection);
        scheduler = new OtpExpiryScheduler(otpCodeDao);
        scheduler.start();

        // Запускаем сервер
        server.setExecutor(null);
        server.start();

        System.out.println("Сервер запущен на порту " + PORT);
        System.out.println("Доступные эндпоинты:");
        System.out.println("  POST   /api/auth/register - Регистрация");
        System.out.println("  POST   /api/auth/login    - Вход");
        System.out.println("  PUT    /api/admin/config  - Обновление конфигурации OTP (ADMIN)");
        System.out.println("  GET    /api/admin/users   - Список пользователей (ADMIN)");
        System.out.println("  DELETE /api/admin/users/{id} - Удаление пользователя (ADMIN)");
        System.out.println("  POST   /api/user/generate - Генерация OTP кода");
        System.out.println("  POST   /api/user/validate - Проверка OTP кода");
        System.out.println("\nФронтенд доступен по адресу: http://localhost:8080");
    }
}