package alexuuport.model;

import java.time.Instant;

public class OtpCode {
    private int id;
    private String operationId;
    private int userId;
    private String code;
    private OtpStatus status;
    private Instant createdAt;
    private Instant expiresAt;
    private String channel;
    private String destination;

    // Конструкторы
    public OtpCode() {}

    public OtpCode(String operationId, int userId, String code, Instant expiresAt, String channel, String destination) {
        this.operationId = operationId;
        this.userId = userId;
        this.code = code;
        this.status = OtpStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
        this.channel = channel;
        this.destination = destination;
    }

    // Геттеры и сеттеры
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public OtpStatus getStatus() {
        return status;
    }

    public void setStatus(OtpStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return status == OtpStatus.ACTIVE && !isExpired();
    }
}
