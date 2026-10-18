package com.bookmyshow.modules.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.UnauthorizedException;
import com.bookmyshow.integrations.google.GoogleTokenVerifier;
import com.bookmyshow.integrations.google.GoogleTokenVerifier.GoogleUser;
import com.bookmyshow.modules.user.UserTypes.AuthResponse;
import com.bookmyshow.modules.user.UserTypes.GoogleLoginRequest;
import com.bookmyshow.modules.user.UserTypes.LoginRequest;
import com.bookmyshow.modules.user.UserTypes.RegisterRequest;
import com.bookmyshow.security.JwtService;
import com.bookmyshow.security.JwtService.IssuedToken;
import com.bookmyshow.security.Roles;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-18T06:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private UserRepository userRepository;
    private UserService userService;
    private PasswordEncoder passwordEncoder;
    private GoogleTokenVerifier googleTokenVerifier;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = mock(UserService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        googleTokenVerifier = mock(GoogleTokenVerifier.class);
        JwtService jwtService = mock(JwtService.class);
        when(passwordEncoder.encode("timing-safe-dummy-password")).thenReturn("dummy-hash");
        when(jwtService.issue(anyLong(), anyString())).thenReturn(new IssuedToken("token", NOW.plusSeconds(3600)));
        when(userRepository.save(any(User.class))).thenAnswer(call -> withId(call.getArgument(0), 1L));
        authService =
                new AuthService(userRepository, userService, passwordEncoder, jwtService, googleTokenVerifier, CLOCK);
    }

    @Test
    void registersWithLowerCaseEmailAndHashedPassword() {
        when(passwordEncoder.encode("secret-pass")).thenReturn("hash");
        when(userService.initialRole("asha@example.com")).thenReturn(Roles.ADMIN);

        AuthResponse response = authService.register(new RegisterRequest("Asha", " Asha@Example.com ", "secret-pass"));

        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("asha@example.com");
        assertThat(response.user().role()).isEqualTo(Roles.ADMIN);
    }

    @Test
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("asha@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Asha", "asha@example.com", "secret-pass")))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void logsInWithRightPassword() {
        User user = withId(User.withPassword("Asha", "asha@example.com", "hash", Roles.USER, NOW), 5L);
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret-pass", "hash")).thenReturn(true);

        assertThat(authService
                        .login(new LoginRequest("asha@example.com", "secret-pass"))
                        .user()
                        .id())
                .isEqualTo(5L);
    }

    @Test
    void rejectsWrongPassword() {
        User user = withId(User.withPassword("Asha", "asha@example.com", "hash", Roles.USER, NOW), 5L);
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("asha@example.com", "wrong")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void unknownEmailStillChecksAHash() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "secret-pass")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
        verify(passwordEncoder).matches("secret-pass", "dummy-hash");
    }

    @Test
    void googleCreatesNewUser() {
        when(googleTokenVerifier.verify("id-token"))
                .thenReturn(new GoogleUser("g-1", "Ravi@Example.com", true, "Ravi Kumar"));
        when(userRepository.findByGoogleSubject("g-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ravi@example.com")).thenReturn(Optional.empty());
        when(userService.initialRole("ravi@example.com")).thenReturn(Roles.USER);

        AuthResponse response = authService.loginWithGoogle(new GoogleLoginRequest("id-token"));

        assertThat(response.user().name()).isEqualTo("Ravi Kumar");
        assertThat(response.user().email()).isEqualTo("ravi@example.com");
    }

    @Test
    void googleLinksExistingEmailAccount() {
        User existing = withId(User.withPassword("Ravi", "ravi@example.com", "hash", Roles.USER, NOW), 9L);
        when(googleTokenVerifier.verify("id-token")).thenReturn(new GoogleUser("g-1", "ravi@example.com", true, null));
        when(userRepository.findByGoogleSubject("g-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ravi@example.com")).thenReturn(Optional.of(existing));

        AuthResponse response = authService.loginWithGoogle(new GoogleLoginRequest("id-token"));

        assertThat(response.user().id()).isEqualTo(9L);
        assertThat(existing.getGoogleSubject()).isEqualTo("g-1");
        verify(userRepository, never()).save(any());
    }

    @Test
    void googleRejectsUnverifiedEmail() {
        when(googleTokenVerifier.verify("id-token"))
                .thenReturn(new GoogleUser("g-1", "ravi@example.com", false, "Ravi"));

        assertThatThrownBy(() -> authService.loginWithGoogle(new GoogleLoginRequest("id-token")))
                .isInstanceOf(UnauthorizedException.class);
    }

    static User withId(User user, long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
