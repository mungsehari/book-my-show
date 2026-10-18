package com.bookmyshow.modules.waitlist;

import com.bookmyshow.common.lock.JobLock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Every minute: waitlist offers nobody took go to the next person in the queue. */
@Component
public class WaitlistOfferJob {

    private static final Logger log = LoggerFactory.getLogger(WaitlistOfferJob.class);

    private final WaitlistService waitlistService;
    private final JobLock jobLock;

    public WaitlistOfferJob(WaitlistService waitlistService, JobLock jobLock) {
        this.waitlistService = waitlistService;
        this.jobLock = jobLock;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void run() {
        if (!jobLock.tryAcquire("waitlist-offers", Duration.ofSeconds(50))) {
            return;
        }
        int expired = waitlistService.expireOffers();
        if (expired > 0) {
            log.info("Expired {} waitlist offers and passed the seats on", expired);
        }
    }
}
