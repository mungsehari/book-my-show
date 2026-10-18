package com.bookmyshow.modules.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookmyshow.bootstrap.AppProperties;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.security.Roles;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        AppProperties properties =
                new AppProperties("Asia/Kolkata", null, null, List.of(" Boss@Example.com ", ""), null, null);
        userService = new UserService(userRepository, properties);
    }

    @Test
    void listedEmailsBecomeAdmin() {
        assertThat(userService.initialRole("boss@example.com")).isEqualTo(Roles.ADMIN);
        assertThat(userService.initialRole("someone@example.com")).isEqualTo(Roles.USER);
        assertThat(userService.initialRole(null)).isEqualTo(Roles.USER);
    }

    @Test
    void grantsTheatreAdminToPlainUser() {
        User user = User.withPhone("Meera", "+919876543210", Roles.USER, Instant.now());
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        userService.grantTheatreAdmin(3L);

        assertThat(user.getRole()).isEqualTo(Roles.THEATRE_ADMIN);
    }

    @Test
    void adminKeepsRole() {
        User user = User.withPhone("Boss", "+919876543210", Roles.ADMIN, Instant.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.grantTheatreAdmin(1L);

        assertThat(user.getRole()).isEqualTo(Roles.ADMIN);
    }

    @Test
    void failsForMissingUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User 99 not found");
    }
}
