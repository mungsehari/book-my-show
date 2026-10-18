package com.bookmyshow.modules.poll;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    List<PollOption> findByPollId(Long pollId);
}
