package com.bookmyshow.modules.booking;

import com.bookmyshow.common.lock.JobLock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Every minute: split bookings not fully paid in time expire, and paid shares are refunded. */
@Component
public class SplitExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(SplitExpiryJob.class);

    private final SplitService splitService;
    private final JobLock jobLock;

    public SplitExpiryJob(SplitService splitService, JobLock jobLock) {
        this.splitService = splitService;
        this.jobLock = jobLock;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 40_000)
    public void run() {
        if (!jobLock.tryAcquire("split-expiry", Duration.ofSeconds(50))) {
            return;
        }
        int expired = splitService.expireStale();
        if (expired > 0) {
            log.info("Expired {} split bookings and refunded paid shares", expired);
        }
    }
}
