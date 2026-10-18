package com.bookmyshow.modules.poll;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.integrations.notify.Notifier;
import com.bookmyshow.modules.friend.FriendService;
import com.bookmyshow.modules.poll.PollTypes.CreatePollRequest;
import com.bookmyshow.modules.poll.PollTypes.PollMemberResponse;
import com.bookmyshow.modules.poll.PollTypes.PollOptionResponse;
import com.bookmyshow.modules.poll.PollTypes.PollResponse;
import com.bookmyshow.modules.show.ShowService;
import com.bookmyshow.modules.show.ShowTypes.ShowResponse;
import com.bookmyshow.modules.user.UserService;
import com.bookmyshow.security.CurrentUser;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Group polls: a user picks 2 to 5 upcoming shows and invites friends to vote. Only members see the poll. The creator
 * closes it, and the show with the most votes wins (the earliest show wins a tie).
 */
@Service
@Transactional(readOnly = true)
public class PollService {

    private final PollRepository pollRepository;
    private final PollOptionRepository optionRepository;
    private final PollMemberRepository memberRepository;
    private final PollVoteRepository voteRepository;
    private final FriendService friendService;
    private final ShowService showService;
    private final UserService userService;
    private final Notifier notifier;
    private final Clock clock;

    public PollService(
            PollRepository pollRepository,
            PollOptionRepository optionRepository,
            PollMemberRepository memberRepository,
            PollVoteRepository voteRepository,
            FriendService friendService,
            ShowService showService,
            UserService userService,
            Notifier notifier,
            Clock clock) {
        this.pollRepository = pollRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.voteRepository = voteRepository;
        this.friendService = friendService;
        this.showService = showService;
        this.userService = userService;
        this.notifier = notifier;
        this.clock = clock;
    }

    @Transactional
    public PollResponse create(CreatePollRequest request, CurrentUser user) {
        Set<Long> showIds = new LinkedHashSet<>(request.showIds());
        if (showIds.size() != request.showIds().size()) {
            throw new BadRequestException("A show is listed more than once");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        for (Long showId : showIds) {
            ShowResponse show = showService.getShow(showId);
            if (!LocalDateTime.of(show.showDate(), show.startTime()).isAfter(now)) {
                throw new BadRequestException("Show " + showId + " has already started");
            }
        }
        Set<Long> friends = friendService.friendIds(user.id());
        Set<Long> invited = new LinkedHashSet<>(request.friendIds());
        invited.remove(user.id());
        for (Long friendId : invited) {
            if (!friends.contains(friendId)) {
                throw new BadRequestException("User " + friendId + " is not your friend");
            }
        }

        Poll poll = pollRepository.save(new Poll(user.id(), request.title().strip(), clock.instant()));
        optionRepository.saveAll(
                showIds.stream().map(id -> new PollOption(poll.getId(), id)).toList());
        List<PollMember> members = new ArrayList<>();
        members.add(new PollMember(poll.getId(), user.id()));
        invited.forEach(id -> members.add(new PollMember(poll.getId(), id)));
        memberRepository.saveAll(members);
        invited.forEach(id -> notifier.notify(
                id,
                "Vote: " + poll.getTitle(),
                "User " + user.id() + " wants you to pick a show (poll " + poll.getId() + ")."));
        return toResponse(poll, user);
    }

    public PollResponse get(long pollId, CurrentUser user) {
        return toResponse(memberPoll(pollId, user), user);
    }

    /** Polls you are in, newest first. */
    public List<PollResponse> mine(CurrentUser user) {
        Set<Long> pollIds = memberRepository.findByUserId(user.id()).stream()
                .map(PollMember::getPollId)
                .collect(Collectors.toSet());
        if (pollIds.isEmpty()) {
            return List.of();
        }
        return pollRepository.findByIdInOrderByCreatedAtDesc(pollIds).stream()
                .map(poll -> toResponse(poll, user))
                .toList();
    }

    /** One vote per member. Voting again changes the vote. */
    @Transactional
    public PollResponse vote(long pollId, long showId, CurrentUser user) {
        Poll poll = memberPoll(pollId, user);
        if (!poll.isOpen()) {
            throw new ConflictException("Poll " + pollId + " is closed");
        }
        boolean isOption =
                optionRepository.findByPollId(pollId).stream().anyMatch(option -> option.getShowId() == showId);
        if (!isOption) {
            throw new BadRequestException("Show " + showId + " is not in this poll");
        }
        voteRepository
                .findByPollIdAndUserId(pollId, user.id())
                .ifPresentOrElse(
                        vote -> vote.change(showId, clock.instant()),
                        () -> voteRepository.save(new PollVote(pollId, user.id(), showId, clock.instant())));
        return toResponse(poll, user);
    }

    @Transactional
    public PollResponse close(long pollId, CurrentUser user) {
        Poll poll = memberPoll(pollId, user);
        if (poll.getCreatorId() != user.id()) {
            throw new ForbiddenException("Only the creator can close this poll");
        }
        if (!poll.isOpen()) {
            return toResponse(poll, user);
        }
        Map<Long, Long> votes = voteCounts(pollId);
        Long winner = optionShows(pollId).stream()
                .filter(show -> votes.getOrDefault(show.id(), 0L) > 0)
                .max(Comparator.<ShowResponse>comparingLong(show -> votes.getOrDefault(show.id(), 0L))
                        .thenComparing(
                                show -> LocalDateTime.of(show.showDate(), show.startTime()), Comparator.reverseOrder()))
                .map(ShowResponse::id)
                .orElse(null);
        poll.close(winner, clock.instant());
        memberRepository.findByPollId(pollId).stream()
                .map(PollMember::getUserId)
                .filter(id -> id != user.id())
                .forEach(id -> notifier.notify(
                        id,
                        "Poll closed: " + poll.getTitle(),
                        winner == null ? "Nobody voted." : "The group picked show " + winner + "."));
        return toResponse(poll, user);
    }

    private Poll memberPoll(long pollId, CurrentUser user) {
        Poll poll = pollRepository
                .findById(pollId)
                .orElseThrow(() -> new NotFoundException("Poll " + pollId + " not found"));
        if (!memberRepository.existsByPollIdAndUserId(pollId, user.id())) {
            throw new ForbiddenException("You are not in this poll");
        }
        return poll;
    }

    private List<ShowResponse> optionShows(long pollId) {
        return optionRepository.findByPollId(pollId).stream()
                .map(option -> showService.getShow(option.getShowId()))
                .toList();
    }

    private Map<Long, Long> voteCounts(long pollId) {
        return voteRepository.findByPollId(pollId).stream()
                .collect(Collectors.groupingBy(PollVote::getShowId, Collectors.counting()));
    }

    private PollResponse toResponse(Poll poll, CurrentUser user) {
        List<PollVote> votes = voteRepository.findByPollId(poll.getId());
        Map<Long, Long> counts =
                votes.stream().collect(Collectors.groupingBy(PollVote::getShowId, Collectors.counting()));
        Set<Long> voted = votes.stream().map(PollVote::getUserId).collect(Collectors.toCollection(HashSet::new));
        Long myVote = votes.stream()
                .filter(vote -> vote.getUserId() == user.id())
                .map(PollVote::getShowId)
                .findFirst()
                .orElse(null);
        List<PollOptionResponse> options = optionShows(poll.getId()).stream()
                .sorted(Comparator.comparing(ShowResponse::showDate).thenComparing(ShowResponse::startTime))
                .map(show -> new PollOptionResponse(
                        show.id(),
                        show.movieName(),
                        show.cinemaId(),
                        show.showDate(),
                        show.startTime(),
                        counts.getOrDefault(show.id(), 0L)))
                .toList();
        List<Long> memberIds = memberRepository.findByPollId(poll.getId()).stream()
                .map(PollMember::getUserId)
                .toList();
        Map<Long, String> names = userService.getNames(memberIds);
        List<PollMemberResponse> members = memberIds.stream()
                .map(id -> new PollMemberResponse(id, names.get(id), voted.contains(id)))
                .toList();
        return new PollResponse(
                poll.getId(),
                poll.getTitle(),
                poll.getStatus(),
                poll.getCreatorId(),
                options,
                members,
                myVote,
                poll.getWinningShowId());
    }
}
