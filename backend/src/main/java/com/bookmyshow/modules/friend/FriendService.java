package com.bookmyshow.modules.friend;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.integrations.notify.Notifier;
import com.bookmyshow.modules.friend.FriendTypes.FriendRequestResponse;
import com.bookmyshow.modules.friend.FriendTypes.FriendResponse;
import com.bookmyshow.modules.user.UserService;
import com.bookmyshow.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Friend requests and the friend list. Used by friends activity and group polls. */
@Service
@Transactional(readOnly = true)
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserService userService;
    private final Notifier notifier;
    private final Clock clock;

    public FriendService(
            FriendshipRepository friendshipRepository, UserService userService, Notifier notifier, Clock clock) {
        this.friendshipRepository = friendshipRepository;
        this.userService = userService;
        this.notifier = notifier;
        this.clock = clock;
    }

    /**
     * Sends a request to the user with this email. If that user had already asked you, you become friends right away.
     */
    @Transactional
    public FriendRequestResponse sendRequest(String email, CurrentUser user) {
        long targetId =
                userService.findIdByEmail(email).orElseThrow(() -> new NotFoundException("No user has this email"));
        if (targetId == user.id()) {
            throw new BadRequestException("You cannot add yourself");
        }
        Instant now = clock.instant();
        Friendship pair = friendshipRepository.findPair(user.id(), targetId).orElse(null);
        if (pair == null) {
            pair = friendshipRepository.save(new Friendship(user.id(), targetId, now));
        } else if (FriendshipStatus.ACCEPTED.equals(pair.getStatus())) {
            throw new ConflictException("You are already friends");
        } else if (FriendshipStatus.PENDING.equals(pair.getStatus())) {
            if (pair.getRequesterId() == user.id()) {
                throw new ConflictException("You already sent a request to this user");
            }
            pair.accept(now);
            notifier.notify(targetId, "Friend request accepted", "You and user " + user.id() + " are now friends.");
            return toRequest(pair, userService.getNames(List.of(pair.getRequesterId())));
        } else {
            pair.reopen(user.id(), targetId, now);
        }
        notifier.notify(targetId, "New friend request", "User " + user.id() + " wants to be your friend.");
        return toRequest(pair, userService.getNames(List.of(user.id())));
    }

    public List<FriendRequestResponse> incoming(CurrentUser user) {
        List<Friendship> pending = friendshipRepository.findByAddresseeIdAndStatusOrderByCreatedAtDesc(
                user.id(), FriendshipStatus.PENDING);
        Map<Long, String> names = userService.getNames(
                pending.stream().map(Friendship::getRequesterId).toList());
        return pending.stream().map(f -> toRequest(f, names)).toList();
    }

    @Transactional
    public FriendRequestResponse accept(long requestId, CurrentUser user) {
        Friendship request = pendingFor(requestId, user);
        request.accept(clock.instant());
        notifier.notify(
                request.getRequesterId(), "Friend request accepted", "User " + user.id() + " accepted your request.");
        return toRequest(request, userService.getNames(List.of(request.getRequesterId())));
    }

    @Transactional
    public FriendRequestResponse decline(long requestId, CurrentUser user) {
        Friendship request = pendingFor(requestId, user);
        request.decline(clock.instant());
        return toRequest(request, userService.getNames(List.of(request.getRequesterId())));
    }

    /** Your friends, sorted by name. */
    public List<FriendResponse> list(CurrentUser user) {
        List<Friendship> accepted = friendshipRepository.findForUser(user.id(), FriendshipStatus.ACCEPTED);
        Map<Long, String> names = userService.getNames(
                accepted.stream().map(f -> f.otherThan(user.id())).toList());
        return accepted.stream()
                .map(f -> new FriendResponse(
                        f.otherThan(user.id()), names.get(f.otherThan(user.id())), f.getRespondedAt()))
                .sorted(Comparator.comparing(FriendResponse::name, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    @Transactional
    public void remove(long friendUserId, CurrentUser user) {
        Friendship pair = friendshipRepository
                .findPair(user.id(), friendUserId)
                .filter(f -> FriendshipStatus.ACCEPTED.equals(f.getStatus()))
                .orElseThrow(() -> new NotFoundException("User " + friendUserId + " is not your friend"));
        pair.remove(clock.instant());
    }

    /** Used by other modules: IDs of the user's friends. */
    public Set<Long> friendIds(long userId) {
        return friendshipRepository.findForUser(userId, FriendshipStatus.ACCEPTED).stream()
                .map(f -> f.otherThan(userId))
                .collect(Collectors.toSet());
    }

    private Friendship pendingFor(long requestId, CurrentUser user) {
        Friendship request = friendshipRepository
                .findById(requestId)
                .filter(f -> FriendshipStatus.PENDING.equals(f.getStatus()))
                .orElseThrow(() -> new NotFoundException("Friend request " + requestId + " not found"));
        if (request.getAddresseeId() != user.id()) {
            throw new ForbiddenException("This request was not sent to you");
        }
        return request;
    }

    private static FriendRequestResponse toRequest(Friendship friendship, Map<Long, String> names) {
        return new FriendRequestResponse(
                friendship.getId(),
                friendship.getRequesterId(),
                names.get(friendship.getRequesterId()),
                friendship.getStatus(),
                friendship.getCreatedAt());
    }
}
