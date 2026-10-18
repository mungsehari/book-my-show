package com.bookmyshow.modules.user;

import com.bookmyshow.bootstrap.AppProperties;
import com.bookmyshow.common.error.TooManyRequestsException;
import com.bookmyshow.common.error.UnauthorizedException;
import com.bookmyshow.integrations.sms.SmsSender;
import com.bookmyshow.modules.user.UserTypes.AuthResponse;
import com.bookmyshow.modules.user.UserTypes.OtpSentResponse;
import com.bookmyshow.security.Roles;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Phone login with a 6-digit code. A new phone number gets a new account. */
@Service
public class OtpService {

    private static final String WRONG_CODE = "Invalid or expired code";

    private final OtpCodeRepository otpCodeRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final SmsSender smsSender;
    private final AppProperties.Otp settings;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public OtpService(
            OtpCodeRepository otpCodeRepository,
            UserRepository userRepository,
            AuthService authService,
            PasswordEncoder passwordEncoder,
            SmsSender smsSender,
            AppProperties properties,
            Clock clock) {
        this.otpCodeRepository = otpCodeRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
        this.smsSender = smsSender;
        this.settings = properties.otp();
        this.clock = clock;
    }

    @Transactional
    public OtpSentResponse requestCode(String phone) {
        Instant now = clock.instant();
        otpCodeRepository.findFirstByPhoneOrderByCreatedAtDesc(phone).ifPresent(last -> {
            if (now.isBefore(last.getCreatedAt().plus(settings.resendAfter()))) {
                throw new TooManyRequestsException("Please wait before asking for a new code");
            }
        });
        String code = String.format("%06d", random.nextInt(1_000_000));
        otpCodeRepository.save(new OtpCode(phone, passwordEncoder.encode(code), now, now.plus(settings.ttl())));
        smsSender.send(
                phone,
                "Your Book My Show code is " + code + ". It is valid for "
                        + settings.ttl().toMinutes() + " minutes.");
        return new OtpSentResponse(phone, settings.ttl().toSeconds());
    }

    /** Checks the code. A wrong code still counts as an attempt, so the attempt is saved even when this fails. */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse verifyCode(String phone, String code, String name) {
        OtpCode otp = otpCodeRepository
                .findFirstByPhoneOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> new UnauthorizedException(WRONG_CODE));
        if (!otp.isUsable(clock.instant(), settings.maxAttempts())) {
            throw new UnauthorizedException(WRONG_CODE);
        }
        otp.recordAttempt();
        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            throw new UnauthorizedException(WRONG_CODE);
        }
        otp.markUsed();
        User user = userRepository
                .findByPhone(phone)
                .orElseGet(() -> userRepository.save(User.withPhone(
                        name == null || name.isBlank() ? "Guest" : name.strip(), phone, Roles.USER, clock.instant())));
        return authService.tokenFor(user);
    }
}
