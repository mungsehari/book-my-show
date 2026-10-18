package com.bookmyshow.modules.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** All request and response types for the user module. */
public final class UserTypes {

    /** International format, for example +919876543210. */
    static final String PHONE_PATTERN = "^\\+[1-9][0-9]{7,14}$";

    private UserTypes() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {}

    public record LoginRequest(
            @NotBlank String email, @NotBlank String password) {}

    public record OtpRequest(
            @NotBlank @Pattern(regexp = PHONE_PATTERN) String phone) {}

    /** Name is used only when the phone signs in for the first time. */
    public record OtpVerifyRequest(
            @NotBlank @Pattern(regexp = PHONE_PATTERN) String phone,
            @NotBlank @Pattern(regexp = "^[0-9]{6}$") String code,
            @Size(max = 100) String name) {}

    public record GoogleLoginRequest(@NotBlank String idToken) {}

    public record OtpSentResponse(String phone, long expiresInSeconds) {}

    public record UserResponse(Long id, String name, String email, String phone, String role) {

        static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole());
        }
    }

    public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UserResponse user) {

        static AuthResponse of(String accessToken, Instant expiresAt, User user) {
            return new AuthResponse(accessToken, "Bearer", expiresAt, UserResponse.from(user));
        }
    }
}
