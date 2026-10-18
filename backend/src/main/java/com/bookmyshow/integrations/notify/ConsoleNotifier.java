package com.bookmyshow.integrations.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Development provider. Writes notifications to the log. Replace with email, SMS, or push before going live. */
@Component
public class ConsoleNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(ConsoleNotifier.class);

    @Override
    public void notify(long userId, String subject, String message) {
        log.info("Notify user {}: {} | {}", userId, subject, message);
    }
}
