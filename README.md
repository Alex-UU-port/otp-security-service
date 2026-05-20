# OTP Security Service - Сервис защиты операций с временными кодами

## О проекте

**OTP Security Service** - это учебное веб-приложение, которое обеспечивает дополнительный уровень безопасности при выполнении различных операций с помощью одноразовых временных кодов (OTP). Сервис позволяет генерировать, отправлять и проверять коды подтверждения через различные каналы связи.
В учебных целях приложение написано на чистой "Java", без использования фреймворков.

### Основные возможности

- Регистрация и аутентификация пользователей с JWT токенами
- Генерация OTP кодов с настраиваемой длиной и временем жизни
- Отправка кодов через 4 канала:
    - Email (реальная отправка или эмуляция)
    - Telegram (реальный бот или эмуляция)
    - SMS (эмулятор SMPP)
    - Файл (сохранение на диск)
- Валидация кодов с проверкой статуса (ACTIVE/EXPIRED/USED)
- Автоматическое истечение просроченных кодов
- Ролевая модель (ADMIN / USER)
- Административная панель для управления пользователями и настройками

### Системные требования

- Java 11 или выше
- PostgreSQL 17
- Gradle (или использовать Gradle Wrapper)
- Telegram аккаунт (для реальной отправки)
- SMTP доступ (для реальной отправки email)

### Установка и запуск

1. В корне проекта запустите 
  ````
  docker compose up -d
  ````
Команда запустит контейнер с Postgres17

2. Создайте БД postgres
  ```
    CREATE DATABASE otp_service;
  ```

3. Далее запустите приложение 
````
./gradlew clean build
./gradlew run
````

При первом запуске приложение инициализирует БД из файла ~/src/main/resources/db/init.sql

Для проверки работы можно использоывть web интерфейс http://localhost:8080 или curl

Доступные эндпоинты:  
POST   /api/auth/register - Регистрация  
POST   /api/auth/login    - Вход  
PUT    /api/admin/config  - Обновление конфигурации OTP (ADMIN)    
GET    /api/admin/users   - Список пользователей (ADMIN)  
DELETE /api/admin/users/{id} - Удаление пользователя (ADMIN)  
POST   /api/user/generate - Генерация OTP кода  
POST   /api/user/validate - Проверка OTP кода  

### Запросы CURL:  

#### Регистрация первого пользователя (станет АДМИНИСТРАТОРОМ):
````
curl -X POST http://localhost:8080/api/auth/register \
-H "Content-Type: application/json" \
-d '{
"username": "admin",
"password": "admin123",
"email": "admin@example.com",
"phone": "+79991234567"
}'
````
#### Регистрация обычного пользователя:
````
curl -X POST http://localhost:8080/api/auth/register \
-H "Content-Type: application/json" \
-d '{
"username": "user1",
"password": "user123",
"email": "user1@example.com",
"phone": "+79997654321"
}'
````
#### Регистрация второго обычного пользователя
````
curl -X POST http://localhost:8080/api/auth/register \
-H "Content-Type: application/json" \
-d '{
"username": "user2",
"password": "user456",
"email": "user2@example.com",
"phone": "+79991112233"
}'
````
#### Вход администратора
````
curl -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{
"username": "admin",
"password": "admin123"
}'
````
#### Вход обычного пользователя
````
curl -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{
"username": "user1",
"password": "user123"
}'
````
#### Получить список всех обычных пользователей (требует права ADMIN)
````
curl -X GET http://localhost:8080/api/admin/users \
-H "Authorization: Bearer $TOKEN_ADMIN"
````
#### Обновить конфигурацию OTP (изменить длину кода и время жизни)
````
# Установить код из 8 цифр, время жизни 180 секунд (3 минуты)
curl -X PUT http://localhost:8080/api/admin/config \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -d '{
    "codeLength": 8,
    "ttlSeconds": 180
  }'
  
  # Установить код из 6 цифр, время жизни 300 секунд (5 минут) - стандартные настройки
curl -X PUT http://localhost:8080/api/admin/config \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -d '{
    "codeLength": 6,
    "ttlSeconds": 300
  }'
  
  # Установить короткий код из 4 цифр на 2 минуты
curl -X PUT http://localhost:8080/api/admin/config \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -d '{
    "codeLength": 4,
    "ttlSeconds": 120
  }'
````
####  Удалить пользователя (требует права ADMIN)
````
# Удалить пользователя с ID=2
curl -X DELETE http://localhost:8080/api/admin/users/2 \
  -H "Authorization: Bearer $TOKEN_ADMIN"
````
#### Генерация OTP кода с отправкой по EMAIL
````
curl -X POST http://localhost:8080/api/user/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_USER" \
  -d '{
    "channel": "email",
    "destination": "user@example.com"
  }'
````
####  Генерация OTP кода с отправкой через TELEGRAM
````
curl -X POST http://localhost:8080/api/user/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_USER" \
  -d '{
    "channel": "telegram",
    "destination": "@username"
  }'
````
#### Генерация OTP кода с отправкой через SMS (эмулятор)
````
curl -X POST http://localhost:8080/api/user/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_USER" \
  -d '{
    "channel": "sms",
    "destination": "+79991234567"
  }'
````
#### Генерация OTP кода с сохранением в ФАЙЛ
````
curl -X POST http://localhost:8080/api/user/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_USER" \
  -d '{
    "channel": "file",
    "destination": "local_storage"
  }'
````
#### Проверка OTP кода (валидация)
````
curl -X POST http://localhost:8080/api/user/validate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_USER" \
  -d '{
    "operationId": "550e8400-e29b-41d4-a716-446655440000",
    "code": "123456"
  }'
````
### Попытка регистрации существующего пользователя
````
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123",
    "email": "admin@example.com"
  }'
````
#### Вход с неверным паролем
````
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user1",
    "password": "wrongpassword"
  }'
````
#### Доступ к админ API без токена
````
curl -X GET http://localhost:8080/api/admin/users
````
#### Доступ к админ API с обычным пользователем
````
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $TOKEN_USER"
````
#### Генерация кода без авторизации
````
curl -X POST http://localhost:8080/api/user/generate \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "email",
    "destination": "test@example.com"
  }'
````
#### Валидация с неверным кодом
````
curl -X POST http://localhost:8080/api/user/validate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_USER" \
  -d '{
    "operationId": "550e8400-e29b-41d4-a716-446655440000",
    "code": "000000"
  }'
````
#### Валидация с несуществующим operationId
````
curl -X POST http://localhost:8080/api/user/validate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_USER" \
  -d '{
    "operationId": "00000000-0000-0000-0000-000000000000",
    "code": "123456"
  }'
````