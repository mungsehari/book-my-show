package com.bookmyshow.modules.friend;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /** The row for a pair, whoever asked. */
    @Query("""
            select f from Friendship f
            where (f.requesterId = :a and f.addresseeId = :b) or (f.requesterId = :b and f.addresseeId = :a)
            """)
    Optional<Friendship> findPair(Long a, Long b);

    /** Rows where the user is on either side, in one status. */
    @Query("""
            select f from Friendship f
            where (f.requesterId = :userId or f.addresseeId = :userId) and f.status = :status
            """)
    List<Friendship> findForUser(Long userId, String status);

    List<Friendship> findByAddresseeIdAndStatusOrderByCreatedAtDesc(Long addresseeId, String status);
}
