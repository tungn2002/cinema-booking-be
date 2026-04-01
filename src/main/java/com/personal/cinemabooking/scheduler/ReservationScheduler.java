package com.personal.cinemabooking.scheduler;

import com.personal.cinemabooking.service.ReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduler for automatic reservation cleanup tasks.
 */
@Component
@Slf4j
public class ReservationScheduler {

    @Autowired
    private ReservationService reservationService;

    /**
     * Expiration time in minutes for confirmed reservations.
     * Reservations older than this will be auto-canceled.
     */
    @Value("${reservation.expiration.minutes:15}")
    private int expirationMinutes;

    /**
     * Runs every 10 minutes to cancel expired confirmed reservations.
     * A reservation is considered expired if:
     * - Status is CONFIRMED (statusId = 1)
     * - Not paid (paid = false)
     * - Created more than 15 minutes ago (configurable via reservation.expiration.minutes)
     */
    @Scheduled(fixedRateString = "${reservation.scheduler.fixed-rate:600000}") // Default: 10 minutes (600000 ms)
    public void autoCancelExpiredReservations() {
        log.info("Starting scheduled auto-cancel job for expired reservations");

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(expirationMinutes);

        try {
            int canceledCount = reservationService.cancelExpiredReservations(cutoffTime);
            log.info("Auto-cancel job completed. Canceled {} reservations", canceledCount);
        } catch (Exception e) {
            log.error("Error during auto-cancel job: {}", e.getMessage(), e);
        }
    }
}
