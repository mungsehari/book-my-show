package com.bookmyshow.integrations.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Development provider. Writes the message to the log instead of sending it. Replace with a real provider (MSG91,
 * Twilio) before going live, because the log then holds login codes.
 */
@Component
public class ConsoleSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsSender.class);

    @Override
    public void send(String phone, String message) {
        log.info("SMS to {}: {}", phone, message);
    }
}
