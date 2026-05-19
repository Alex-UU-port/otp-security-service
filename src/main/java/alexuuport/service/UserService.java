package alexuuport.service;

import com.otp.dao.UserDao;
import com.otp.dao.OtpCodeDao;
import com.otp.model.User;
import com.otp.util.PasswordUtil;
import com.otp.util.LoggerUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class UserService {
    private final UserDao userDao;
    private final OtpCodeDao otpCodeDao;
    private final LoggerUtil logger;

    public UserService(Connection connection) {
        this.userDao = new UserDao(connection);
        this.otpCodeDao = new OtpCodeDao(connection);
        this.logger = new LoggerUtil(UserService.class);
    }

    /**
     * Регистрация нового пользователя
     * @param username имя пользователя
     * @param password пароль
     * @param email email адрес
     * @param phone номер телефона
     * @param telegramId Telegram ID
     * @return зарегистрированный пользователь
     * @throws Exception если пользователь уже существует или ошибка БД
     */
    public User registerUser(String username, String password, String email, String phone, String telegramId) throws Exception {
        logger.info("Попытка регистрации пользователя: {}", username);

        // Проверяем, существует ли пользователь
        User existingUser = userDao.findByUsername(username);
        if (existingUser != null) {
            logger.warn("Попытка регистрации существующего пользователя: {}", username);
            throw new Exception("Пользователь с таким именем уже существует");
        }

        // Определяем роль (первый пользователь становится администратором)
        String role;
        if (!userDao.hasAdminExists()) {
            role = "ADMIN";
            logger.info("Создание первого администратора: {}", username);
        } else {
            role = "USER";
        }

        // Хешируем пароль
        String passwordHash = PasswordUtil.hashPassword(password);

        // Создаем пользователя
        User user = new User(username, passwordHash, role, email, phone, telegramId);
        boolean created = userDao.createUser(user);

        if (created) {
            logger.success("Пользователь успешно зарегистрирован: {} с ролью {}", username, role);
            return user;
        } else {
            logger.error("Не удалось создать пользователя: {}", username);
            throw new Exception("Ошибка при создании пользователя");
        }
    }

    /**
     * Аутентификация пользователя
     * @param username имя пользователя
     * @param password пароль
     * @return пользователь если аутентификация успешна
     * @throws Exception если неверные учетные данные
     */
    public User authenticateUser(String username, String password) throws Exception {
        logger.info("Попытка аутентификации пользователя: {}", username);

        User user = userDao.findByUsername(username);
        if (user == null) {
            logger.warn("Попытка входа несуществующего пользователя: {}", username);
            throw new Exception("Пользователь не найден");
        }

        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            logger.warn("Неверный пароль для пользователя: {}", username);
            throw new Exception("Неверный пароль");
        }

        logger.success("Пользователь успешно аутентифицирован: {}", username);
        return user;
    }

    /**
     * Получение всех обычных пользователей (не администраторов)
     * @return список пользователей
     * @throws SQLException ошибка БД
     */
    public List<User> getAllNonAdminUsers() throws SQLException {
        logger.info("Запрос списка обычных пользователей");
        List<User> users = userDao.getAllNonAdminUsers();
        logger.debug("Найдено {} обычных пользователей", users.size());
        return users;
    }

    /**
     * Удаление пользователя по ID
     * @param userId ID пользователя
     * @return true если удаление успешно
     * @throws SQLException ошибка БД
     */
    public boolean deleteUser(int userId) throws SQLException {
        logger.info("Попытка удаления пользователя с ID: {}", userId);
        boolean deleted = userDao.deleteUser(userId);
        if (deleted) {
            logger.success("Пользователь с ID {} успешно удален", userId);
        } else {
            logger.warn("Пользователь с ID {} не найден или является администратором", userId);
        }
        return deleted;
    }

    /**
     * Поиск пользователя по имени
     * @param username имя пользователя
     * @return пользователь или null
     * @throws SQLException ошибка БД
     */
    public User findByUsername(String username) throws SQLException {
        return userDao.findByUsername(username);
    }

    /**
     * Проверка существования администратора
     * @return true если администратор существует
     * @throws SQLException ошибка БД
     */
    public boolean hasAdminExists() throws SQLException {
        return userDao.hasAdminExists();
    }
}
