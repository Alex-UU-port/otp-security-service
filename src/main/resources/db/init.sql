-- Создание таблицы пользователей
CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    telegram_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Создание таблицы конфигурации OTP (всегда одна запись)
CREATE TABLE IF NOT EXISTS otp_config (
                                          id SERIAL PRIMARY KEY,
                                          code_length INT DEFAULT 6,
                                          ttl_seconds INT DEFAULT 300,
                                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы OTP кодов
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
    );

-- Создание индексов
CREATE INDEX idx_otp_codes_operation_id ON otp_codes(operation_id);
CREATE INDEX idx_otp_codes_status ON otp_codes(status);
CREATE INDEX idx_otp_codes_expires_at ON otp_codes(expires_at);

-- Вставка начальной конфигурации
INSERT INTO otp_config (code_length, ttl_seconds) VALUES (6, 300);