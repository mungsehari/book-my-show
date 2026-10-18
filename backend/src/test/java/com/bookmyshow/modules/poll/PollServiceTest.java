package com.bookmyshow.modules.poll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.integrations.notify.Notifier;
import com.bookmyshow.modules.friend.FriendService;
import com.bookmyshow.modules.poll.PollTypes.CreatePollRequest;
import com.bookmyshow.modules.poll.PollTypes.PollResponse;
import com.bookmyshow.modules.show.ShowService;
import com.bookmyshow.modules.show.ShowTypes.ShowResponse;
import com.bookmyshow.modules.user.UserService;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PollServiceTest {

    private static final CurrentUser ASHA = new CurrentUser(7L, Roles.USER);
    private static final CurrentUser RAVI = new CurrentUser(8L, Roles.USER);

    private PollRepository pollRepository;
    private PollOptionRepository optionRepository;
    private PollMemberRepository memberRepository;
    private PollVoteRepository voteRepository;
    private FriendService friendService;
    private ShowService showService;
    private PollService pollService;

    @BeforeEach
    void setUp() {
        pollRepository = mock(PollRepository.class);
        optionRepository = mock(PollOptionRepository.class);
        memberRepository = mock(PollMemberRepository.class);
        voteRepository = mock(PollVoteRepository.class);
        friendService = mock(FriendService.class);
        showService = mock(ShowService.class);
        UserService userService = mock(UserService.class);
        when(userService.getNames(anyCollection())).thenReturn(Map.of(7L, "Asha", 8L, "Ravi"));
        when(showService.getShow(50L)).thenReturn(show(50L, 18));
        when(showService.getShow(51L)).thenReturn(show(51L, 21));
        pollService = new PollService(
                pollRepository,
                optionRepository,
                memberRepository,
                voteRepository,
                friendService,
                showService,
                userService,
                mock(Notifier.class),
                Clock.fixed(Instant.parse("2026-10-18T05:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createsPollWithFriends() {
        when(friendService.friendIds(7L)).thenReturn(Set.of(8L));
        when(pollRepository.save(any(Poll.class))).thenAnswer(call -> withId(call.getArgument(0)));
        when(optionRepository.findByPollId(1L)).thenReturn(options());
        when(memberRepository.findByPollId(1L)).thenReturn(List.of(new PollMember(1L, 7L), new PollMember(1L, 8L)));

        PollResponse poll = pollService.create(new CreatePollRequest("Friday", List.of(50L, 51L), List.of(8L)), ASHA);

        assertThat(poll.options()).hasSize(2);
        assertThat(poll.members()).extracting("userId").containsExactly(7L, 8L);
    }

    @Test
    void onlyFriendsCanBeInvited() {
        when(friendService.friendIds(7L)).thenReturn(Set.of());

        assertThatThrownBy(
                        () -> pollService.create(new CreatePollRequest("Friday", List.of(50L, 51L), List.of(9L)), ASHA))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("User 9 is not your friend");
    }

    @Test
    void nonMemberCannotSeePoll() {
        when(pollRepository.findById(1L)).thenReturn(Optional.of(withId(new Poll(7L, "Friday", Instant.now()))));
        when(memberRepository.existsByPollIdAndUserId(1L, 8L)).thenReturn(false);

        assertThatThrownBy(() -> pollService.get(1L, RAVI)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void voteMustBeForAnOption() {
        member(8L);
        when(optionRepository.findByPollId(1L)).thenReturn(options());

        assertThatThrownBy(() -> pollService.vote(1L, 99L, RAVI)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void votingAgainChangesVote() {
        member(8L);
        when(optionRepository.findByPollId(1L)).thenReturn(options());
        PollVote earlier = new PollVote(1L, 8L, 50L, Instant.now());
        when(voteRepository.findByPollIdAndUserId(1L, 8L)).thenReturn(Optional.of(earlier));

        pollService.vote(1L, 51L, RAVI);

        assertThat(earlier.getShowId()).isEqualTo(51L);
    }

    @Test
    void mostVotesWinsAndTieGoesToEarlierShow() {
        Poll poll = member(7L);
        when(optionRepository.findByPollId(1L)).thenReturn(options());
        when(voteRepository.findByPollId(1L))
                .thenReturn(
                        List.of(new PollVote(1L, 7L, 51L, Instant.now()), new PollVote(1L, 8L, 50L, Instant.now())));

        assertThat(pollService.close(1L, ASHA).winningShowId()).isEqualTo(50L);
        assertThat(poll.isOpen()).isFalse();
    }

    @Test
    void onlyCreatorCloses() {
        member(8L);

        assertThatThrownBy(() -> pollService.close(1L, RAVI)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void closedPollTakesNoVotes() {
        Poll poll = member(8L);
        poll.close(null, Instant.now());

        assertThatThrownBy(() -> pollService.vote(1L, 50L, RAVI)).isInstanceOf(ConflictException.class);
    }

    private Poll member(long userId) {
        Poll poll = withId(new Poll(7L, "Friday", Instant.now()));
        when(pollRepository.findById(1L)).thenReturn(Optional.of(poll));
        when(memberRepository.existsByPollIdAndUserId(1L, userId)).thenReturn(true);
        when(memberRepository.findByPollId(1L)).thenReturn(List.of(new PollMember(1L, 7L), new PollMember(1L, 8L)));
        when(voteRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));
        return poll;
    }

    private static List<PollOption> options() {
        return List.of(new PollOption(1L, 50L), new PollOption(1L, 51L));
    }

    private static Poll withId(Poll poll) {
        ReflectionTestUtils.setField(poll, "id", 1L);
        return poll;
    }

    private static ShowResponse show(long id, int hour) {
        return new ShowResponse(
                id,
                10L,
                3L,
                2L,
                "Sky Riders",
                LocalDate.of(2026, 10, 20),
                LocalTime.of(hour, 0),
                new BigDecimal("200.00"),
                false,
                false,
                false);
    }
}
