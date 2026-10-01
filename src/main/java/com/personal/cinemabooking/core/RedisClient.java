package com.personal.cinemabooking.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class RedisClient {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private ObjectMapper mapper;

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public String get(String key, String defaultValue) {
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? value : defaultValue;
    }

    public <T> T getObject(String key, Class<T> objectType) {
        try {
            String value = get(key);
            if (value == null) {
                return null;
            }
            return mapper.readValue(value, objectType);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, String value, long seconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(seconds));
    }

    public <T> void setObject(String key, T object) {
        try {
            redisTemplate.opsForValue().set(key, mapper.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public <T> void setObject(String key, T object, long seconds) {
        try {
            redisTemplate.opsForValue().set(key, mapper.writeValueAsString(object), Duration.ofSeconds(seconds));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
    
    public void deleteByPattern(String pattern) {
        redisTemplate.delete(redisTemplate.keys(pattern));
    }
}
