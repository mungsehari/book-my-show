package com.bookmyshow.integrations.sms;

/** Sends a text message. Modules use this interface only, never a vendor SDK. */
public interface SmsSender {

    void send(String phone, String message);
}
