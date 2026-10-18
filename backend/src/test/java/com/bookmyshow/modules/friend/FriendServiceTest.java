package com.bookmyshow.modules.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.integrations.notify.Notifier;
import com.bookmyshow.modules.friend.FriendTypes.FriendRequestResponse;
import com.bookmyshow.modules.user.UserService;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FriendServiceTest {

    private static final CurrentUser ASHA = new CurrentUser(7L, Roles.USER);
    private static final Instant NOW = Instant.parse("2026-10-18T05:00:00Z");

    private FriendshipRepository friendshipRepository;
    private UserService userService;
    private FriendService friendService;

    @BeforeEach
    void setUp() {
        friendshipRepository = mock(FriendshipRepository.class);
        userService = mock(UserService.class);
        when(userService.getNames(anyCollection())).thenReturn(Map.of(7L, "Asha", 8L, "Ravi"));
        friendService = new FriendService(
                friendshipRepository, userService, mock(Notifier.class), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void sendsRequestByEmail() {
        when(userService.findIdByEmail("ravi@example.com")).thenReturn(Optional.of(8L));
        when(friendshipRepository.findPair(7L, 8L)).thenReturn(Optional.empty());
        when(friendshipRepository.save(any(Friendship.class))).thenAnswer(call -> withId(call.getArgument(0), 1L));

        FriendRequestResponse response = friendService.sendRequest("ravi@example.com", ASHA);

        assertThat(response.status()).isEqualTo(FriendshipStatus.PENDING);
        assertThat(response.fromUserId()).isEqualTo(7L);
    }

    @Test
    void requestBackAcceptsRightAway() {
        Friendship theirs = withId(new Friendship(8L, 7L, NOW.minusSeconds(60)), 1L);
        when(userService.findIdByEmail("ravi@example.com")).thenReturn(Optional.of(8L));
        when(friendshipRepository.findPair(7L, 8L)).thenReturn(Optional.of(theirs));

        assertThat(friendService.sendRequest("ravi@example.com", ASHA).status()).isEqualTo(FriendshipStatus.ACCEPTED);
    }

    @Test
    void cannotAskTwice() {
        Friendship mine = withId(new Friendship(7L, 8L, NOW.minusSeconds(60)), 1L);
        when(userService.findIdByEmail("ravi@example.com")).thenReturn(Optional.of(8L));
        when(friendshipRepository.findPair(7L, 8L)).thenReturn(Optional.of(mine));

        assertThatThrownBy(() -> friendService.sendRequest("ravi@example.com", ASHA))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void declinedPairCanBeAskedAgain() {
        Friendship declined = withId(new Friendship(8L, 7L, NOW.minusSeconds(600)), 1L);
        declined.decline(NOW.minusSeconds(300));
        when(userService.findIdByEmail("ravi@example.com")).thenReturn(Optional.of(8L));
        when(friendshipRepository.findPair(7L, 8L)).thenReturn(Optional.of(declined));

        friendService.sendRequest("ravi@example.com", ASHA);

        assertThat(declined.getStatus()).isEqualTo(FriendshipStatus.PENDING);
        assertThat(declined.getRequesterId()).isEqualTo(7L);
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void cannotAddYourself() {
        when(userService.findIdByEmail("asha@example.com")).thenReturn(Optional.of(7L));

        assertThatThrownBy(() -> friendService.sendRequest("asha@example.com", ASHA))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void unknownEmailIs404() {
        when(userService.findIdByEmail("x@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendService.sendRequest("x@example.com", ASHA))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void onlyAddresseeAccepts() {
        Friendship request = withId(new Friendship(8L, 9L, NOW), 1L);
        when(friendshipRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> friendService.accept(1L, ASHA)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void listsFriendsOnBothSides() {
        Friendship asked = withId(new Friendship(7L, 8L, NOW), 1L);
        asked.accept(NOW);
        when(friendshipRepository.findForUser(7L, FriendshipStatus.ACCEPTED)).thenReturn(List.of(asked));

        assertThat(friendService.list(ASHA)).singleElement().satisfies(f -> {
            assertThat(f.userId()).isEqualTo(8L);
            assertThat(f.name()).isEqualTo("Ravi");
        });
        assertThat(friendService.friendIds(7L)).containsExactly(8L);
    }

    private static Friendship withId(Friendship friendship, long id) {
        ReflectionTestUtils.setField(friendship, "id", id);
        return friendship;
    }
}
