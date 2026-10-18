package com.bookmyshow.modules.booking;

import com.bookmyshow.common.lock.JobLock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Every minute: unpaid checkouts past the payment timeout become EXPIRED. */
@Component
public class BookingExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryJob.class);

    private final BookingService bookingService;
    private final JobLock jobLock;

    public BookingExpiryJob(BookingService bookingService, JobLock jobLock) {
        this.bookingService = bookingService;
        this.jobLock = jobLock;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 20_000)
    public void run() {
        if (!jobLock.tryAcquire("booking-expiry", Duration.ofSeconds(50))) {
            return;
        }
        int expired = bookingService.expireStale();
        if (expired > 0) {
            log.info("Expired {} unpaid bookings", expired);
        }
    }
}
