package alexuuport.service;

import org.jsmpp.bean.*;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import com.otp.util.LoggerUtil;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class SmsService {
    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddress;
    private final LoggerUtil logger;

    public SmsService() {
        this.logger = new LoggerUtil(SmsService.class);
        Properties props = loadConfig();
        this.host = props.getProperty("smpp.host", "localhost");
        this.port = Integer.parseInt(props.getProperty("smpp.port", "2775"));
        this.systemId = props.getProperty("smpp.system_id", "smppclient1");
        this.password = props.getProperty("smpp.password", "password");
        this.systemType = props.getProperty("smpp.system_type", "OTP");
        this.sourceAddress = props.getProperty("smpp.source_addr", "OTPService");

        logger.info("SMS сервис инициализирован: хост={}, порт={}, systemId={}", host, port, systemId);
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try {
            // Загружаем из файла, если он существует
            var inputStream = SmsService.class.getClassLoader().getResourceAsStream("sms.properties");
            if (inputStream != null) {
                props.load(inputStream);
                logger.debug("Конфигурация SMS загружена из файла sms.properties");
            } else {
                // Используем значения по умолчанию
                logger.warn("Файл sms.properties не найден, используются значения по умолчанию");
                props.setProperty("smpp.host", "localhost");
                props.setProperty("smpp.port", "2775");
                props.setProperty("smpp.system_id", "smppclient1");
                props.setProperty("smpp.password", "password");
                props.setProperty("smpp.system_type", "OTP");
                props.setProperty("smpp.source_addr", "OTPService");
            }
        } catch (Exception e) {
            logger.error("Не удалось загрузить конфигурацию SMS", e);
        }
        return props;
    }

    /**
     * Отправка SMS через эмулятор SMPP сервера
     * @param phoneNumber номер телефона получателя
     * @param code OTP код для отправки
     */
    public void sendCode(String phoneNumber, String code) {
        logger.info("Попытка отправки SMS на номер {} с кодом {}", phoneNumber, code);

        SMPPSession session = new SMPPSession();

        try {
            // Параметры подключения к SMPP серверу
            BindParameter bindParameter = new BindParameter(
                    BindType.BIND_TX,
                    systemId,
                    password,
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress
            );

            // Подключаемся к SMPP серверу
            logger.debug("Подключение к SMPP серверу {}:{}", host, port);
            session.connectAndBind(host, port, bindParameter);
            logger.debug("Успешное подключение к SMPP серверу");

            // Формируем сообщение
            String messageText = String.format("Ваш код подтверждения: %s. Код действителен 5 минут.", code);
            byte[] messageBytes = messageText.getBytes(StandardCharsets.UTF_8);

            // Отправляем сообщение
            String messageId = session.submitShortMessage(
                    systemType,                    // systemType
                    TypeOfNumber.UNKNOWN,          // sourceAddrTon
                    NumberingPlanIndicator.UNKNOWN, // sourceAddrNpi
                    sourceAddress,                 // sourceAddr
                    TypeOfNumber.UNKNOWN,          // destAddrTon
                    NumberingPlanIndicator.UNKNOWN, // destAddrNpi
                    phoneNumber,                   // destinationAddr
                    new ESMClass(),                // esmClass
                    (byte) 0,                      // protocolId
                    (byte) 1,                      // priorityFlag
                    null,                          // scheduleDeliveryTime
                    null,                          // validityPeriod
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT), // registeredDelivery
                    (byte) 0,                      // replaceIfPresentFlag
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT), // dataCoding
                    (byte) 0,                      // smDefaultMsgId
                    messageBytes                   // shortMessage
            );

            logger.success("SMS успешно отправлена на номер {}", phoneNumber);
            logger.debug("ID сообщения SMPP: {}", messageId);

        } catch (Exception e) {
            logger.error("Ошибка при отправке SMS на номер {}: {}", phoneNumber, e.getMessage(), e);

            // Эмуляция успешной отправки для тестирования без реального SMPP сервера
            logger.warn("Режим эмуляции: SMS не отправлена, но код сгенерирован");
            simulateSmsDelivery(phoneNumber, code);

        } finally {
            try {
                if (session.getSessionState().isBound()) {
                    session.unbindAndClose();
                    logger.debug("Соединение с SMPP сервером закрыто");
                }
            } catch (Exception e) {
                logger.error("Ошибка при закрытии SMPP соединения", e);
            }
        }
    }

    /**
     * Эмуляция отправки SMS для тестирования
     */
    private void simulateSmsDelivery(String phoneNumber, String code) {
        String simulatedOutput = String.format(
                "\n========================================\n" +
                        "📱 ЭМУЛЯЦИЯ SMS ОТПРАВКИ\n" +
                        "Кому: %s\n" +
                        "Код подтверждения: %s\n" +
                        "Сообщение: Ваш код подтверждения: %s. Код действителен 5 минут.\n" +
                        "========================================\n",
                phoneNumber, code, code
        );
        System.out.println(simulatedOutput);

        // Сохраняем эмуляцию в файл лога
        try {
            java.nio.file.Files.writeString(
                    java.nio.file.Paths.get("sms_emulation.log"),
                    java.time.LocalDateTime.now() + " - " + simulatedOutput + "\n",
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            logger.error("Ошибка записи в лог эмуляции SMS", e);
        }
    }

    /**
     * Проверка статуса SMS сервера
     * @return true если сервер доступен
     */
    public boolean checkSmsServerStatus() {
        SMPPSession session = new SMPPSession();
        try {
            BindParameter bindParameter = new BindParameter(
                    BindType.BIND_TX,
                    systemId,
                    password,
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress
            );
            session.connectAndBind(host, port, bindParameter);
            session.unbindAndClose();
            logger.info("SMPP сервер доступен");
            return true;
        } catch (Exception e) {
            logger.warn("SMPP сервер недоступен: {}", e.getMessage());
            return false;
        }
    }
}