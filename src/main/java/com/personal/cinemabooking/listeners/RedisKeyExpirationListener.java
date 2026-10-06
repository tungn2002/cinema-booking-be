package com.personal.cinemabooking.listeners;

import com.personal.cinemabooking.services.ReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RedisKeyExpirationListener extends KeyExpirationEventMessageListener {

    private final ReservationService reservationService;

    public RedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer,
                                      ReservationService reservationService) {
        super(listenerContainer);
        this.reservationService = reservationService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        log.info("Redis key expired: {}", expiredKey);

        // Format: showtime:{showtimeId}:seat:{seatId}
        if (expiredKey.startsWith("showtime:") && expiredKey.contains(":seat:")) {
            try {
                String[] parts = expiredKey.split(":");
                if (parts.length == 4) {
                    Long showtimeId = Long.parseLong(parts[1]);
                    Long seatId = Long.parseLong(parts[3]);
                    log.info("Processing expired seat lock for showtime {}, seat {}", showtimeId, seatId);
                    
                    reservationService.handleSeatExpiration(showtimeId, seatId);
                }
            } catch (Exception e) {
                log.error("Error processing expired key: {}", expiredKey, e);
            }
        }
    }
}
