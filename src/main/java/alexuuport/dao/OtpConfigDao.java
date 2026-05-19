package alexuuport.dao;

import alexuuport.model.OtpConfig;
import java.sql.*;

public class OtpConfigDao {
    private final Connection connection;

    public OtpConfigDao(Connection connection) {
        this.connection = connection;
    }

    public OtpConfig getConfig() throws SQLException {
        String sql = "SELECT * FROM otp_config LIMIT 1";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                OtpConfig config = new OtpConfig();
                config.setId(rs.getInt("id"));
                config.setCodeLength(rs.getInt("code_length"));
                config.setTtlSeconds(rs.getInt("ttl_seconds"));
                config.setUpdatedAt(rs.getTimestamp("updated_at"));
                return config;
            }
            // Если конфигурации нет, создаем дефолтную
            OtpConfig defaultConfig = new OtpConfig();
            defaultConfig.setCodeLength(6);
            defaultConfig.setTtlSeconds(300);
            updateConfig(defaultConfig);
            return defaultConfig;
        }
    }

    public void updateConfig(OtpConfig config) throws SQLException {
        String sql = "UPDATE otp_config SET code_length = ?, ttl_seconds = ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, config.getCodeLength());
            stmt.setInt(2, config.getTtlSeconds());
            stmt.executeUpdate();
        }
    }
}