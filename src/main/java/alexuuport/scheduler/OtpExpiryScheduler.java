package alexuuport.scheduler;

import com.otp.dao.OtpCodeDao;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OtpExpiryScheduler {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final OtpCodeDao otpCodeDao;

    public OtpExpiryScheduler(OtpCodeDao otpCodeDao) {
        this.otpCodeDao = otpCodeDao;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                otpCodeDao.expireOldCodes();
                System.out.println("Проверка просроченных OTP кодов выполнена");
            } catch (Exception e) {
                System.err.println("Ошибка при проверке просроченных кодов: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    public void stop() {
        scheduler.shutdown();
    }
}