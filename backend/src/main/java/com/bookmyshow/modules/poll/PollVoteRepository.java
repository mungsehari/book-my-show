package com.bookmyshow.modules.poll;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    List<PollVote> findByPollId(Long pollId);

    Optional<PollVote> findByPollIdAndUserId(Long pollId, Long userId);
}
