package com.bookmyshow.integrations.notify;

/** Tells a user about something (waitlist offer, refund). Modules use this interface only, never a vendor SDK. */
public interface Notifier {

    void notify(long userId, String subject, String message);
}
