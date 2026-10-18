package com.bookmyshow.modules.show;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Seat locks in Redis. Each locked seat is a key that expires by itself, so a user who leaves never blocks seats for
 * long.
 *
 * <p>Keys:
 *
 * <ul>
 *   <li>{@code seatlock:show:{showId}:seat:{seatId}} holds the lock ID.
 *   <li>{@code seatlock:lock:{lockId}} holds "userId|showId|seatId,seatId".
 *   <li>{@code seatlock:user:{userId}:show:{showId}} holds the lock ID, so a user has one lock per show.
 * </ul>
 *
 * <p>Lua scripts make lock and release all-or-nothing, so two users can never hold the same seat.
 */
@Component
class SeatLockStore {

    /** KEYS: seat keys, then user key, then lock key. ARGV: lock ID, TTL in ms, lock value. */
    private static final DefaultRedisScript<Long> LOCK = new DefaultRedisScript<>("""
            local seats = #KEYS - 2
            if redis.call('EXISTS', KEYS[seats + 1]) == 1 then return -1 end
            for i = 1, seats do
              if redis.call('EXISTS', KEYS[i]) == 1 then return 0 end
            end
            for i = 1, seats do
              redis.call('SET', KEYS[i], ARGV[1], 'PX', ARGV[2])
            end
            redis.call('SET', KEYS[seats + 1], ARGV[1], 'PX', ARGV[2])
            redis.call('SET', KEYS[seats + 2], ARGV[3], 'PX', ARGV[2])
            return 1
            """, Long.class);

    /** KEYS: seat keys, then user key, then lock key. ARGV: lock ID. Frees only keys that still hold this lock. */
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>("""
            local freed = 0
            for i = 1, #KEYS - 1 do
              if redis.call('GET', KEYS[i]) == ARGV[1] then
                redis.call('DEL', KEYS[i])
                freed = freed + 1
              end
            end
            redis.call('DEL', KEYS[#KEYS])
            return freed
            """, Long.class);

    private final StringRedisTemplate redis;

    SeatLockStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    LockResult lock(String lockId, long userId, long showId, List<Long> seatIds, Duration ttl) {
        List<String> keys = new ArrayList<>(seatKeys(showId, seatIds));
        keys.add(userKey(userId, showId));
        keys.add(lockKey(lockId));
        String value = userId + "|" + showId + "|" + join(seatIds);
        Long result = redis.execute(LOCK, keys, lockId, Long.toString(ttl.toMillis()), value);
        if (result == null || result == 0) {
            return LockResult.SEAT_TAKEN;
        }
        return result < 0 ? LockResult.USER_HAS_LOCK : LockResult.LOCKED;
    }

    Optional<HeldLock> find(String lockId) {
        String value = redis.opsForValue().get(lockKey(lockId));
        if (value == null) {
            return Optional.empty();
        }
        String[] parts = value.split("\\|");
        List<Long> seatIds = new ArrayList<>();
        for (String id : parts[2].split(",")) {
            seatIds.add(Long.parseLong(id));
        }
        return Optional.of(new HeldLock(lockId, Long.parseLong(parts[0]), Long.parseLong(parts[1]), seatIds));
    }

    void release(HeldLock lock) {
        List<String> keys = new ArrayList<>(seatKeys(lock.showId(), lock.seatIds()));
        keys.add(userKey(lock.userId(), lock.showId()));
        keys.add(lockKey(lock.lockId()));
        redis.execute(RELEASE, keys, lock.lockId());
    }

    /** Seat IDs from the given list that are locked right now, by anyone. */
    Set<Long> lockedSeats(long showId, Collection<Long> seatIds) {
        if (seatIds.isEmpty()) {
            return Set.of();
        }
        List<Long> ordered = List.copyOf(seatIds);
        List<String> values = redis.opsForValue().multiGet(seatKeys(showId, ordered));
        Set<Long> locked = new HashSet<>();
        for (int i = 0; i < ordered.size(); i++) {
            if (values != null && values.get(i) != null) {
                locked.add(ordered.get(i));
            }
        }
        return locked;
    }

    private static List<String> seatKeys(long showId, Collection<Long> seatIds) {
        return seatIds.stream()
                .map(seatId -> "seatlock:show:" + showId + ":seat:" + seatId)
                .toList();
    }

    private static String userKey(long userId, long showId) {
        return "seatlock:user:" + userId + ":show:" + showId;
    }

    private static String lockKey(String lockId) {
        return "seatlock:lock:" + lockId;
    }

    private static String join(List<Long> ids) {
        return String.join(",", ids.stream().map(String::valueOf).toList());
    }

    enum LockResult {
        LOCKED,
        SEAT_TAKEN,
        USER_HAS_LOCK
    }

    record HeldLock(String lockId, long userId, long showId, List<Long> seatIds) {}
}
