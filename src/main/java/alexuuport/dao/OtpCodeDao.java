package alexuuport.dao;

import com.otp.model.OtpCode;
import com.otp.model.OtpStatus;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OtpCodeDao {
    private final Connection connection;

    public OtpCodeDao(Connection connection) {
        this.connection = connection;
    }

    public void saveOtpCode(OtpCode otpCode) throws SQLException {
        String sql = "INSERT INTO otp_codes (operation_id, user_id, code, status, expires_at, channel, destination) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, otpCode.getOperationId());
            stmt.setInt(2, otpCode.getUserId());
            stmt.setString(3, otpCode.getCode());
            stmt.setString(4, otpCode.getStatus().name());
            stmt.setTimestamp(5, Timestamp.from(otpCode.getExpiresAt()));
            stmt.setString(6, otpCode.getChannel());
            stmt.setString(7, otpCode.getDestination());
            stmt.executeUpdate();
        }
    }

    public OtpCode findByOperationId(String operationId) throws SQLException {
        String sql = "SELECT * FROM otp_codes WHERE operation_id = ? ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, operationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapOtpCode(rs);
            }
            return null;
        }
    }

    public void updateStatus(int id, OtpStatus status) throws SQLException {
        String sql = "UPDATE otp_codes SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public void expireOldCodes() throws SQLException {
        String sql = "UPDATE otp_codes SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND expires_at < CURRENT_TIMESTAMP";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    public List<OtpCode> getCodesByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM otp_codes WHERE user_id = ? ORDER BY created_at DESC";
        List<OtpCode> codes = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                codes.add(mapOtpCode(rs));
            }
        }
        return codes;
    }

    private OtpCode mapOtpCode(ResultSet rs) throws SQLException {
        OtpCode otpCode = new OtpCode();
        otpCode.setId(rs.getInt("id"));
        otpCode.setOperationId(rs.getString("operation_id"));
        otpCode.setUserId(rs.getInt("user_id"));
        otpCode.setCode(rs.getString("code"));
        otpCode.setStatus(OtpStatus.valueOf(rs.getString("status")));
        otpCode.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        otpCode.setExpiresAt(rs.getTimestamp("expires_at").toInstant());
        otpCode.setChannel(rs.getString("channel"));
        otpCode.setDestination(rs.getString("destination"));
        return otpCode;
    }
}