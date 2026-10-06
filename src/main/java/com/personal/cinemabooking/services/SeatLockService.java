package com.personal.cinemabooking.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatLockService {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    // LUA Script: Check if key exists. If not, set it with TTL and return 1 (success). Else return 0 (fail).
    private static final String LOCK_SCRIPT = 
            "if redis.call('exists', KEYS[1]) == 0 then " +
            "   redis.call('set', KEYS[1], ARGV[1], 'EX', ARGV[2]); " +
            "   return 1; " +
            "else " +
            "   return 0; " +
            "end;";

    public boolean lockSeat(Long showtimeId, Long seatId, String username) {
        String key = "showtime:" + showtimeId + ":seat:" + seatId;
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LOCK_SCRIPT);
        script.setResultType(Long.class);

        // Lock for 10 minutes (600 seconds)
        Long result = redisTemplate.execute(script, Collections.singletonList(key), username, "600");
        
        if (result != null && result == 1L) {
            // Success - Broadcast via WebSocket
            broadcastSeatStatus(showtimeId, seatId, "LOCKED");
            return true;
        }
        return false;
    }
    
    public void unlockSeat(Long showtimeId, Long seatId) {
        String key = "showtime:" + showtimeId + ":seat:" + seatId;
        redisTemplate.delete(key);
        broadcastSeatStatus(showtimeId, seatId, "AVAILABLE");
    }

    public void broadcastSeatStatus(Long showtimeId, Long seatId, String status) {
        String destination = "/topic/showtimes/" + showtimeId + "/seats";
        String message = String.format("{\"seatId\": %d, \"status\": \"%s\"}", seatId, status);
        messagingTemplate.convertAndSend(destination, message);
    }
}
