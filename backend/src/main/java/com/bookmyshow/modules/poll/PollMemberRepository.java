package com.bookmyshow.modules.poll;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface PollMemberRepository extends JpaRepository<PollMember, Long> {

    List<PollMember> findByPollId(Long pollId);

    List<PollMember> findByUserId(Long userId);

    boolean existsByPollIdAndUserId(Long pollId, Long userId);
}
