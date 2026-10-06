package com.personal.cinemabooking.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> tokenBucketScript;

    public boolean isAllowed(String ip, int capacity, int refillRatePerSecond) {
        String key = "ratelimit:" + ip;
        long now = Instant.now().getEpochSecond();
        
        Long result = redisTemplate.execute(
            tokenBucketScript,
            Collections.singletonList(key),
            String.valueOf(capacity),
            String.valueOf(refillRatePerSecond),
            String.valueOf(now),
            "1"
        );
        
        return result != null && result == 1L;
    }
}
