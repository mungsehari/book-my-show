package com.bookmyshow.common.lock;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * A short Redis lock for scheduled jobs. When several backend instances run, only the one that gets the lock runs the
 * job this round. The lock expires by itself, so a crashed instance never blocks the job for long.
 */
@Component
public class JobLock {

    private final StringRedisTemplate redis;

    public JobLock(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** True if this instance may run the job now. */
    public boolean tryAcquire(String jobName, Duration ttl) {
        Boolean acquired = redis.opsForValue().setIfAbsent("joblock:" + jobName, "1", ttl);
        return Boolean.TRUE.equals(acquired);
    }
}
