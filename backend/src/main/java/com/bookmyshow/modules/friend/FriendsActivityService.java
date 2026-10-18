package com.bookmyshow.modules.friend;

import com.bookmyshow.modules.booking.BookingService;
import com.bookmyshow.modules.booking.BookingTypes.MovieAudience;
import com.bookmyshow.modules.friend.FriendTypes.FriendSummary;
import com.bookmyshow.modules.friend.FriendTypes.FriendsActivityResponse;
import com.bookmyshow.modules.movie.MovieService;
import com.bookmyshow.modules.user.UserService;
import com.bookmyshow.security.CurrentUser;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** "3 of your friends watched this." Only friends who share their activity are counted. */
@Service
@Transactional(readOnly = true)
public class FriendsActivityService {

    private final FriendService friendService;
    private final UserService userService;
    private final BookingService bookingService;
    private final MovieService movieService;

    public FriendsActivityService(
            FriendService friendService,
            UserService userService,
            BookingService bookingService,
            MovieService movieService) {
        this.friendService = friendService;
        this.userService = userService;
        this.bookingService = bookingService;
        this.movieService = movieService;
    }

    public FriendsActivityResponse forMovie(long movieId, CurrentUser user) {
        movieService.ensureExists(movieId);
        Set<Long> sharing = userService.withActivitySharing(friendService.friendIds(user.id()));
        MovieAudience audience = bookingService.audienceOf(movieId, sharing);
        Map<Long, String> names = userService.getNames(sharing);
        List<FriendSummary> watched = summaries(audience.watched(), names);
        List<FriendSummary> going = summaries(audience.going(), names);
        return new FriendsActivityResponse(movieId, watched.size(), watched, going.size(), going);
    }

    private static List<FriendSummary> summaries(Set<Long> userIds, Map<Long, String> names) {
        return userIds.stream()
                .map(id -> new FriendSummary(id, names.get(id)))
                .sorted(Comparator.comparing(FriendSummary::name, Comparator.nullsLast(String::compareTo)))
                .toList();
    }
}
