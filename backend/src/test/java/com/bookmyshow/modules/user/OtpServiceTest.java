package com.bookmyshow.modules.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.bootstrap.AppProperties;
import com.bookmyshow.common.error.TooManyRequestsException;
import com.bookmyshow.common.error.UnauthorizedException;
import com.bookmyshow.integrations.sms.SmsSender;
import com.bookmyshow.modules.user.UserTypes.AuthResponse;
import com.bookmyshow.modules.user.UserTypes.OtpSentResponse;
import com.bookmyshow.security.Roles;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class OtpServiceTest {

    private static final String PHONE = "+919876543210";
    private static final Instant NOW = Instant.parse("2026-10-18T06:00:00Z");

    private OtpCodeRepository otpCodeRepository;
    private UserRepository userRepository;
    private AuthService authService;
    private PasswordEncoder passwordEncoder;
    private SmsSender smsSender;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpCodeRepository = mock(OtpCodeRepository.class);
        userRepository = mock(UserRepository.class);
        authService = mock(AuthService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        smsSender = mock(SmsSender.class);
        AppProperties properties = new AppProperties(
                "Asia/Kolkata",
                null,
                null,
                List.of(),
                new AppProperties.Otp(Duration.ofMinutes(5), 5, Duration.ofSeconds(60)),
                null);
        otpService = new OtpService(
                otpCodeRepository,
                userRepository,
                authService,
                passwordEncoder,
                smsSender,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void sendsCodeAndStoresOnlyItsHash() {
        when(otpCodeRepository.findFirstByPhoneOrderByCreatedAtDesc(PHONE)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("code-hash");

        OtpSentResponse response = otpService.requestCode(PHONE);

        assertThat(response.expiresInSeconds()).isEqualTo(300);
        ArgumentCaptor<OtpCode> saved = ArgumentCaptor.forClass(OtpCode.class);
        verify(otpCodeRepository).save(saved.capture());
        assertThat(saved.getValue().getCodeHash()).isEqualTo("code-hash");
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(smsSender).send(eq(PHONE), message.capture());
        assertThat(message.getValue()).containsPattern("code is [0-9]{6}\\.");
    }

    @Test
    void refusesNewCodeTooSoon() {
        OtpCode recent = new OtpCode(PHONE, "hash", NOW.minusSeconds(30), NOW.plusSeconds(270));
        when(otpCodeRepository.findFirstByPhoneOrderByCreatedAtDesc(PHONE)).thenReturn(Optional.of(recent));

        assertThatThrownBy(() -> otpService.requestCode(PHONE)).isInstanceOf(TooManyRequestsException.class);
        verify(smsSender, never()).send(anyString(), anyString());
    }

    @Test
    void wrongCodeCountsAsAttempt() {
        OtpCode otp = new OtpCode(PHONE, "hash", NOW.minusSeconds(10), NOW.plusSeconds(290));
        when(otpCodeRepository.findFirstByPhoneOrderByCreatedAtDesc(PHONE)).thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("111111", "hash")).thenReturn(false);

        assertThatThrownBy(() -> otpService.verifyCode(PHONE, "111111", null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid or expired code");
        assertThat(otp.getAttempts()).isEqualTo(1);
        assertThat(otp.getUsed()).isFalse();
    }

    @Test
    void expiredCodeFails() {
        OtpCode otp = new OtpCode(PHONE, "hash", NOW.minusSeconds(400), NOW.minusSeconds(100));
        when(otpCodeRepository.findFirstByPhoneOrderByCreatedAtDesc(PHONE)).thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> otpService.verifyCode(PHONE, "123456", null))
                .isInstanceOf(UnauthorizedException.class);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void codeLocksAfterMaxAttempts() {
        OtpCode otp = new OtpCode(PHONE, "hash", NOW.minusSeconds(10), NOW.plusSeconds(290));
        for (int i = 0; i < 5; i++) {
            otp.recordAttempt();
        }
        when(otpCodeRepository.findFirstByPhoneOrderByCreatedAtDesc(PHONE)).thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> otpService.verifyCode(PHONE, "123456", null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rightCodeCreatesAccountForNewPhone() {
        OtpCode otp = new OtpCode(PHONE, "hash", NOW.minusSeconds(10), NOW.plusSeconds(290));
        when(otpCodeRepository.findFirstByPhoneOrderByCreatedAtDesc(PHONE)).thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(call -> AuthServiceTest.withId(call.getArgument(0), 3L));
        AuthResponse expected = new AuthResponse("token", "Bearer", NOW, null);
        when(authService.tokenFor(any(User.class))).thenReturn(expected);

        assertThat(otpService.verifyCode(PHONE, "123456", " Meera ")).isSameAs(expected);
        assertThat(otp.getUsed()).isTrue();
        ArgumentCaptor<User> created = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(created.capture());
        assertThat(created.getValue().getName()).isEqualTo("Meera");
        assertThat(created.getValue().getRole()).isEqualTo(Roles.USER);
    }
}
