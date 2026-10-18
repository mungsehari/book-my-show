package com.bookmyshow.modules.user;

import com.bookmyshow.bootstrap.AppProperties;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.user.UserTypes.UserResponse;
import com.bookmyshow.security.Roles;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** User lookups and role changes. Used by the auth services and by other modules. */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final List<String> adminEmails;

    public UserService(UserRepository userRepository, AppProperties properties) {
        this.userRepository = userRepository;
        this.adminEmails = properties.adminEmails();
    }

    public UserResponse getUser(long userId) {
        return UserResponse.from(find(userId));
    }

    /** Used by other modules to check a user before using its ID. */
    public void ensureExists(long userId) {
        if (!userRepository.existsById(userId)) {
            throw notFound(userId);
        }
    }

    /** Makes a plain user a theatre admin. Admins keep their role. */
    @Transactional
    public void grantTheatreAdmin(long userId) {
        User user = find(userId);
        if (Roles.USER.equals(user.getRole())) {
            user.changeRole(Roles.THEATRE_ADMIN);
        }
    }

    /** Role for a new account. Emails listed in ADMIN_EMAILS become ADMIN. */
    String initialRole(String email) {
        return email != null && adminEmails.contains(email) ? Roles.ADMIN : Roles.USER;
    }

    private User find(long userId) {
        return userRepository.findById(userId).orElseThrow(() -> notFound(userId));
    }

    private static NotFoundException notFound(long userId) {
        return new NotFoundException("User " + userId + " not found");
    }
}
