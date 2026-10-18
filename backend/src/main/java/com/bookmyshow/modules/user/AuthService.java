package com.bookmyshow.modules.user;

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
import java.time.Clock;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Email and password sign-up and login, and Google sign-in. Phone login lives in OtpService. */
@Service
@Transactional
public class AuthService {

    private static final String WRONG_LOGIN = "Invalid email or password";

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final Clock clock;

    /** Checked when the email is unknown, so a wrong email takes as long as a wrong password. */
    private final String dummyHash;

    public AuthService(
            UserRepository userRepository,
            UserService userService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            GoogleTokenVerifier googleTokenVerifier,
            Clock clock) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("timing-safe-dummy-password");
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        User user = userRepository.save(User.withPassword(
                request.name().strip(),
                email,
                passwordEncoder.encode(request.password()),
                userService.initialRole(email),
                clock.instant()));
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email())).orElse(null);
        String hash = user != null && user.getPasswordHash() != null ? user.getPasswordHash() : dummyHash;
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user == null || user.getPasswordHash() == null || !matches) {
            throw new UnauthorizedException(WRONG_LOGIN);
        }
        return tokenFor(user);
    }

    /** Signs in with Google. Links to an existing account with the same verified email, or creates one. */
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUser google = googleTokenVerifier.verify(request.idToken());
        if (!google.emailVerified() || google.email() == null) {
            throw new UnauthorizedException("Google account email is not verified");
        }
        String email = normalizeEmail(google.email());
        User user = userRepository
                .findByGoogleSubject(google.subject())
                .orElseGet(() -> userRepository
                        .findByEmail(email)
                        .map(existing -> link(existing, google.subject()))
                        .orElseGet(() -> userRepository.save(User.withGoogle(
                                displayName(google.name(), email),
                                email,
                                google.subject(),
                                userService.initialRole(email),
                                clock.instant()))));
        return tokenFor(user);
    }

    AuthResponse tokenFor(User user) {
        IssuedToken token = jwtService.issue(user.getId(), user.getRole());
        return AuthResponse.of(token.value(), token.expiresAt(), user);
    }

    private static User link(User user, String googleSubject) {
        if (user.getGoogleSubject() != null && !user.getGoogleSubject().equals(googleSubject)) {
            throw new ConflictException("Email is linked to another Google account");
        }
        user.linkGoogle(googleSubject);
        return user;
    }

    private static String displayName(String name, String email) {
        String value = name == null || name.isBlank() ? email.substring(0, email.indexOf('@')) : name.strip();
        return value.length() > 100 ? value.substring(0, 100) : value;
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
