package com.bookmyshow.integrations.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Claude API settings from {@code app.claude.*}. A blank API key turns AI off; keyword search still works. */
@ConfigurationProperties("app.claude")
public record ClaudeProperties(String apiKey, String model) {

    public static final String DEFAULT_MODEL = "claude-opus-5-5";

    public ClaudeProperties {
        model = model == null || model.isBlank() ? DEFAULT_MODEL : model.strip();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
