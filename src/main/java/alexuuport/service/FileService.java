package alexuuport.service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileService {
    private static final String FILE_PATH = "otp_codes.txt";

    public void saveCodeToFile(String operationId, String code, String destination, String channel) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH, true))) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.printf("[%s] Операция: %s | Канал: %s | Получатель: %s | Код: %s%n",
                    timestamp, operationId, channel, destination, code);
            writer.flush();
            System.out.println("Код сохранен в файл: " + FILE_PATH);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения кода в файл: " + e.getMessage());
        }
    }
}