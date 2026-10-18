package com.bookmyshow.modules.alert;

import com.bookmyshow.common.lock.JobLock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Every 5 minutes: checks active price alerts against live prices. */
@Component
public class PriceAlertJob {

    private static final Logger log = LoggerFactory.getLogger(PriceAlertJob.class);

    private final PriceAlertService alertService;
    private final JobLock jobLock;

    public PriceAlertJob(PriceAlertService alertService, JobLock jobLock) {
        this.alertService = alertService;
        this.jobLock = jobLock;
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void run() {
        if (!jobLock.tryAcquire("price-alerts", Duration.ofMinutes(4))) {
            return;
        }
        int fired = alertService.runChecks();
        if (fired > 0) {
            log.info("Sent {} price alerts", fired);
        }
    }
}
