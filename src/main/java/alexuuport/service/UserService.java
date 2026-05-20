
//На данный момент это лишний класс, все эти функции реализованы в AuthHandler


package alexuuport.service;

import alexuuport.dao.UserDao;
import alexuuport.dao.OtpCodeDao;
import alexuuport.model.User;
import alexuuport.util.PasswordUtil;
import alexuuport.util.LoggerUtil;

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

    public List<User> getAllNonAdminUsers() throws SQLException {
        logger.info("Запрос списка обычных пользователей");
        List<User> users = userDao.getAllNonAdminUsers();
        logger.debug("Найдено {} обычных пользователей", users.size());
        return users;
    }

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

    public User findByUsername(String username) throws SQLException {
        return userDao.findByUsername(username);
    }

    public boolean hasAdminExists() throws SQLException {
        return userDao.hasAdminExists();
    }
}
