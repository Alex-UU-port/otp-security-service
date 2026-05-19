package alexuuport.service;

import com.otp.dao.OtpCodeDao;
import com.otp.dao.OtpConfigDao;
import com.otp.model.OtpCode;
import com.otp.model.OtpConfig;
import com.otp.model.OtpStatus;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

public class OtpService {
    private final OtpCodeDao otpCodeDao;
    private final OtpConfigDao otpConfigDao;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpCodeDao otpCodeDao, OtpConfigDao otpConfigDao) {
        this.otpCodeDao = otpCodeDao;
        this.otpConfigDao = otpConfigDao;
    }

    public String generateCode(int length) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    public OtpCode createOtpCode(int userId, String channel, String destination) throws Exception {
        OtpConfig config = otpConfigDao.getConfig();
        String operationId = UUID.randomUUID().toString();
        String code = generateCode(config.getCodeLength());

        OtpCode otpCode = new OtpCode();
        otpCode.setOperationId(operationId);
        otpCode.setUserId(userId);
        otpCode.setCode(code);
        otpCode.setStatus(OtpStatus.ACTIVE);
        otpCode.setExpiresAt(Instant.now().plusSeconds(config.getTtlSeconds()));
        otpCode.setChannel(channel);
        otpCode.setDestination(destination);

        otpCodeDao.saveOtpCode(otpCode);
        return otpCode;
    }

    public boolean validateCode(String operationId, String inputCode) throws Exception {
        OtpCode otpCode = otpCodeDao.findByOperationId(operationId);

        if (otpCode == null) {
            return false;
        }

        if (otpCode.getStatus() != OtpStatus.ACTIVE) {
            return false;
        }

        if (Instant.now().isAfter(otpCode.getExpiresAt())) {
            otpCodeDao.updateStatus(otpCode.getId(), OtpStatus.EXPIRED);
            return false;
        }

        if (otpCode.getCode().equals(inputCode)) {
            otpCodeDao.updateStatus(otpCode.getId(), OtpStatus.USED);
            return true;
        }

        return false;
    }
}