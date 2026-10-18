package com.bookmyshow.modules.poll;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface PollRepository extends JpaRepository<Poll, Long> {

    List<Poll> findByIdInOrderByCreatedAtDesc(Collection<Long> ids);
}
