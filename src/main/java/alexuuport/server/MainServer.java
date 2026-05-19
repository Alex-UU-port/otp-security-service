package com.otp.server;

import alexuuport.scheduler.OtpExpiryScheduler;
import alexuuport.server.handlers.AdminHandler;
import alexuuport.server.handlers.AuthHandler;
import alexuuport.server.handlers.UserHandler;
import alexuuport.util.LoggerUtil;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class  MainServer {
    private static final int PORT = 8080;
    private static Connection connection;
    private static OtpExpiryScheduler scheduler;
    private static LoggerUtil logger;

    public static void main(String[] args) throws Exception {
        logger = new LoggerUtil(MainServer.class);
        logger.info("Запуск OTP Security Service...");

        // Загружаем драйвер PostgreSQL
        Class.forName("org.postgresql.Driver");
        logger.debug("Драйвер PostgreSQL загружен");

        // Подключаемся к базе данных
        String url = "jdbc:postgresql://localhost:5432/otp_service";
        String user = "admin";
        String password = "admin123";

        try {
            connection = DriverManager.getConnection(url, user, password);
            logger.success("Подключение к базе данных PostgreSQL установлено");

            // Инициализация таблиц, если их нет
            initializeDatabase();

        } catch (Exception e) {
            logger.error("Ошибка подключения к базе данных", e);
            logger.warn("Убедитесь, что PostgreSQL запущен и база данных otp_service существует");
            throw e;
        }

        // Создаем HTTP сервер
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        logger.info("HTTP сервер создан на порту {}", PORT);

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
                String mimeType = "text/html; charset=UTF-8";
                if (path.endsWith(".css")) mimeType = "text/css; charset=UTF-8";
                if (path.endsWith(".js")) mimeType = "application/javascript; charset=UTF-8";

                exchange.getResponseHeaders().set("Content-Type", mimeType);
                exchange.sendResponseHeaders(200, file.length());
                try (var os = exchange.getResponseBody()) {
                    Files.copy(file.toPath(), os);
                }
            } else {
                String response = "Страница не найдена";
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
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
        logger.info("Планировщик проверки просроченных кодов запущен");

        // Запускаем сервер
        server.setExecutor(null);
        server.start();

        logger.success("========================================");
        logger.success("СЕРВЕР УСПЕШНО ЗАПУЩЕН!");
        logger.success("========================================");
        logger.info("Порт: {}", PORT);
        logger.info("URL: http://localhost:{}", PORT);
        logger.info("Фронтенд: http://localhost:{}/", PORT);
        logger.info("");
        logger.info("Доступные эндпоинты:");
        logger.info("  POST   /api/auth/register - Регистрация");
        logger.info("  POST   /api/auth/login    - Вход");
        logger.info("  PUT    /api/admin/config  - Обновление конфигурации OTP (ADMIN)");
        logger.info("  GET    /api/admin/users   - Список пользователей (ADMIN)");
        logger.info("  DELETE /api/admin/users/{id} - Удаление пользователя (ADMIN)");
        logger.info("  POST   /api/user/generate - Генерация OTP кода");
        logger.info("  POST   /api/user/validate - Проверка OTP кода");
        logger.info("========================================");
    }

    private static void initializeDatabase() {
        try {
            // Проверяем, существует ли таблица users
            var checkTableStmt = connection.prepareStatement(
                    "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'users')"
            );
            var rs = checkTableStmt.executeQuery();
            rs.next();
            boolean tablesExist = rs.getBoolean(1);

            if (!tablesExist) {
                logger.info("Таблицы не найдены, выполняем инициализацию базы данных...");

                // Читаем SQL скрипт инициализации
                var inputStream = MainServer.class.getClassLoader().getResourceAsStream("db/init.sql");
                if (inputStream != null) {
                    String sql = new String(inputStream.readAllBytes());
                    try (Statement stmt = connection.createStatement()) {
                        // Разделяем SQL на отдельные команды
                        for (String command : sql.split(";")) {
                            if (!command.trim().isEmpty()) {
                                stmt.execute(command);
                            }
                        }
                        logger.success("База данных успешно инициализирована");
                    }
                } else {
                    logger.warn("Файл init.sql не найден, создаем таблицы вручную...");
                    createTablesManually();
                }
            } else {
                logger.debug("Таблицы уже существуют, пропускаем инициализацию");
            }
        } catch (Exception e) {
            logger.error("Ошибка при инициализации базы данных", e);
        }
    }

    private static void createTablesManually() throws Exception {
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                username VARCHAR(100) UNIQUE NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                role VARCHAR(20) NOT NULL,
                email VARCHAR(255),
                phone VARCHAR(20),
                telegram_id VARCHAR(100),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createOtpConfigTable = """
            CREATE TABLE IF NOT EXISTS otp_config (
                id SERIAL PRIMARY KEY,
                code_length INT DEFAULT 6,
                ttl_seconds INT DEFAULT 300,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createOtpCodesTable = """
            CREATE TABLE IF NOT EXISTS otp_codes (
                id SERIAL PRIMARY KEY,
                operation_id VARCHAR(100) NOT NULL,
                user_id INT REFERENCES users(id),
                code VARCHAR(10) NOT NULL,
                status VARCHAR(20) DEFAULT 'ACTIVE',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP NOT NULL,
                channel VARCHAR(20),
                destination VARCHAR(255)
            )
        """;

        String insertDefaultConfig = """
            INSERT INTO otp_config (code_length, ttl_seconds) 
            SELECT 6, 300 
            WHERE NOT EXISTS (SELECT 1 FROM otp_config)
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createOtpConfigTable);
            stmt.execute(createOtpCodesTable);
            stmt.execute(insertDefaultConfig);
            logger.success("Таблицы созданы вручную");
        }
    }
}