package com.bookmyshow.modules.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookmyshow.modules.booking.BookingService;
import com.bookmyshow.modules.booking.BookingTypes.MovieAudience;
import com.bookmyshow.modules.friend.FriendTypes.FriendsActivityResponse;
import com.bookmyshow.modules.movie.MovieService;
import com.bookmyshow.modules.user.UserService;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FriendsActivityServiceTest {

    @Test
    void countsOnlyFriendsWhoShare() {
        FriendService friendService = mock(FriendService.class);
        UserService userService = mock(UserService.class);
        BookingService bookingService = mock(BookingService.class);
        when(friendService.friendIds(7L)).thenReturn(Set.of(8L, 9L, 10L));
        // User 10 hides activity.
        when(userService.withActivitySharing(Set.of(8L, 9L, 10L))).thenReturn(Set.of(8L, 9L));
        when(bookingService.audienceOf(2L, Set.of(8L, 9L))).thenReturn(new MovieAudience(Set.of(8L), Set.of(9L)));
        when(userService.getNames(Set.of(8L, 9L))).thenReturn(Map.of(8L, "Ravi", 9L, "Meera"));

        FriendsActivityResponse activity = new FriendsActivityService(
                        friendService, userService, bookingService, mock(MovieService.class))
                .forMovie(2L, new CurrentUser(7L, Roles.USER));

        assertThat(activity.watchedCount()).isEqualTo(1);
        assertThat(activity.watched().get(0).name()).isEqualTo("Ravi");
        assertThat(activity.goingCount()).isEqualTo(1);
        assertThat(activity.going().get(0).name()).isEqualTo("Meera");
    }
}
