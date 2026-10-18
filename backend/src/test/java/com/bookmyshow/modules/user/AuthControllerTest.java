package com.bookmyshow.modules.user;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.TestLogins;
import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.common.error.UnauthorizedException;
import com.bookmyshow.modules.user.UserTypes.AuthResponse;
import com.bookmyshow.modules.user.UserTypes.LoginRequest;
import com.bookmyshow.modules.user.UserTypes.OtpSentResponse;
import com.bookmyshow.modules.user.UserTypes.RegisterRequest;
import com.bookmyshow.modules.user.UserTypes.UserResponse;
import com.bookmyshow.security.Roles;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithSecurityConfig
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    private static final UserResponse ASHA = new UserResponse(1L, "Asha", "asha@example.com", null, Roles.USER);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private OtpService otpService;

    @MockitoBean
    private UserService userService;

    @Test
    void registersWithoutToken() throws Exception {
        when(authService.register(new RegisterRequest("Asha", "asha@example.com", "secret-pass")))
                .thenReturn(new AuthResponse("token", "Bearer", Instant.parse("2026-10-18T07:00:00Z"), ASHA));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Asha", "email": "asha@example.com", "password": "secret-pass"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("token"))
                .andExpect(jsonPath("$.user.email").value("asha@example.com"));
    }

    @Test
    void rejectsBadEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Asha", "email": "not-an-email", "password": "secret-pass"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("Invalid value for 'email'")));
    }

    @Test
    void wrongLoginReturns401() throws Exception {
        when(authService.login(new LoginRequest("asha@example.com", "wrong")))
                .thenThrow(new UnauthorizedException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "asha@example.com", "password": "wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void requestsOtp() throws Exception {
        when(otpService.requestCode("+919876543210")).thenReturn(new OtpSentResponse("+919876543210", 300));

        mockMvc.perform(post("/api/v1/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone": "+919876543210"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.expiresInSeconds").value(300));
    }

    @Test
    void rejectsPhoneWithoutCountryCode() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone": "9876543210"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void meNeedsToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Login required"));
    }

    @Test
    void meReturnsLoggedInUser() throws Exception {
        when(userService.getUser(1L)).thenReturn(ASHA);

        mockMvc.perform(get("/api/v1/auth/me").with(TestLogins.as(1L, Roles.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Asha"));
    }
}
