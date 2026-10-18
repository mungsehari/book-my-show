package com.bookmyshow.modules.user;

import com.bookmyshow.modules.user.UserTypes.AuthResponse;
import com.bookmyshow.modules.user.UserTypes.GoogleLoginRequest;
import com.bookmyshow.modules.user.UserTypes.LoginRequest;
import com.bookmyshow.modules.user.UserTypes.OtpRequest;
import com.bookmyshow.modules.user.UserTypes.OtpSentResponse;
import com.bookmyshow.modules.user.UserTypes.OtpVerifyRequest;
import com.bookmyshow.modules.user.UserTypes.RegisterRequest;
import com.bookmyshow.modules.user.UserTypes.UserResponse;
import com.bookmyshow.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final UserService userService;

    public AuthController(AuthService authService, OtpService otpService, UserService userService) {
        this.authService = authService;
        this.otpService = otpService;
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Sign up with email and password")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email and password")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/otp/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Send a 6-digit login code to a phone")
    public OtpSentResponse requestOtp(@Valid @RequestBody OtpRequest request) {
        return otpService.requestCode(request.phone());
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Log in with a phone code. A new phone gets a new account.")
    public AuthResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return otpService.verifyCode(request.phone(), request.code(), request.name());
    }

    @PostMapping("/google")
    @Operation(summary = "Log in with a Google ID token from the frontend")
    public AuthResponse google(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.loginWithGoogle(request);
    }

    @GetMapping("/me")
    @Operation(summary = "The logged-in user", security = @SecurityRequirement(name = "bearer"))
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getUser(CurrentUser.from(jwt).id());
    }
}
