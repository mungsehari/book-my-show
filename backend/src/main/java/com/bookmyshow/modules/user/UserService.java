package com.bookmyshow.modules.user;

import com.bookmyshow.bootstrap.AppProperties;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.user.UserTypes.UserResponse;
import com.bookmyshow.security.Roles;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

    /** Used by other modules to find a user by exact email (friend requests, gifts). Case does not matter. */
    public Optional<Long> findIdByEmail(String email) {
        return userRepository
                .findByEmail(email.strip().toLowerCase(Locale.ROOT))
                .map(User::getId);
    }

    /** Used by other modules: user ID to display name. Unknown IDs are left out. */
    public Map<Long, String> getNames(Collection<Long> userIds) {
        return userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, User::getName));
    }

    /** Used by friends activity: the users from the list who share their movie activity. */
    public Set<Long> withActivitySharing(Collection<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .filter(User::isShareActivity)
                .map(User::getId)
                .collect(Collectors.toSet());
    }

    public boolean isSharingActivity(long userId) {
        return find(userId).isShareActivity();
    }

    @Transactional
    public boolean setShareActivity(long userId, boolean shareActivity) {
        User user = find(userId);
        user.setShareActivity(shareActivity);
        return user.isShareActivity();
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
