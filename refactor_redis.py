import os
import re

base_pkg = "c:/Users/ASUS/Desktop/mypj/cinema-booking-be/src/main/java/com/personal/cinemabooking"

# 1. Update RedisConfig.java
redis_config_content = """package com.personal.cinemabooking.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }
}
"""
with open(f"{base_pkg}/configs/RedisConfig.java", "w", encoding="utf-8") as f:
    f.write(redis_config_content)

# 2. Create RedisClient.java
redis_client_content = """package com.personal.cinemabooking.core;

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
"""
with open(f"{base_pkg}/core/RedisClient.java", "w", encoding="utf-8") as f:
    f.write(redis_client_content)

# 3. Modify MovieService.java
movie_service_path = f"{base_pkg}/services/MovieService.java"
with open(movie_service_path, "r", encoding="utf-8") as f:
    ms_content = f.read()

# Add imports for RedisClient
if "import com.personal.cinemabooking.core.RedisClient;" not in ms_content:
    ms_content = ms_content.replace("import org.springframework.stereotype.Service;", "import org.springframework.stereotype.Service;\nimport com.personal.cinemabooking.core.RedisClient;")

# Remove Cache annotations imports
ms_content = re.sub(r'import org\.springframework\.cache\.annotation\..*?;\n', '', ms_content)

# Inject RedisClient
if "private final RedisClient redisClient;" not in ms_content:
    # Find constructor
    ms_content = re.sub(
        r'(public MovieService\([^)]+)\)\s*\{',
        r'\1, RedisClient redisClient) {',
        ms_content
    )
    # Add field
    ms_content = ms_content.replace("private final MovieRepository movieRepository;", "private final MovieRepository movieRepository;\n    private final RedisClient redisClient;")
    # Add assignment in constructor
    ms_content = ms_content.replace("this.modelMapper = modelMapper;", "this.modelMapper = modelMapper;\n        this.redisClient = redisClient;")

# Remove Cacheable from getMovieById
# We need to replace the method body
get_movie_regex = re.compile(r'@Cacheable\(value = "movies", key = "#id"\)\s*public MovieDTO getMovieById\(Long id\) \{([\s\S]*?)return mapToDTO\(movie\);\s*\}')
get_movie_match = get_movie_regex.search(ms_content)
if get_movie_match:
    new_method = """public MovieDTO getMovieById(Long id) {
        String cacheKey = "movie:" + id;
        MovieDTO cached = redisClient.getObject(cacheKey, MovieDTO.class);
        if (cached != null) {
            return cached;
        }

        log.info("Fetching movie with id: {}", id);
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
        MovieDTO result = mapToDTO(movie);
        redisClient.setObject(cacheKey, result, 3600); // cache for 1 hour
        return result;
    }"""
    ms_content = ms_content[:get_movie_match.start()] + new_method + ms_content[get_movie_match.end():]

# Remove CachePut from updateMovie
update_movie_regex = re.compile(r'@CachePut\(value = "movies", key = "#id"\)\s*public MovieDTO updateMovie\(Long id, MovieRequest movieRequest, MultipartFile image\) throws IOException \{')
if update_movie_regex.search(ms_content):
    ms_content = update_movie_regex.sub("public MovieDTO updateMovie(Long id, MovieRequest movieRequest, MultipartFile image) throws IOException {", ms_content)
    # Update the return statement of updateMovie to set cache
    # Since updateMovie is long, let's just replace the `return mapToDTO(updatedMovie);` with cache update
    ms_content = re.sub(r'return mapToDTO\(updatedMovie\);', r'MovieDTO result = mapToDTO(updatedMovie);\n        redisClient.setObject("movie:" + id, result, 3600);\n        return result;', ms_content, count=1)

# Remove CacheEvict from deleteMovie
delete_movie_regex = re.compile(r'@CacheEvict\(value = "movies", key = "#id"\)\s*public void deleteMovie\(Long id\) \{')
if delete_movie_regex.search(ms_content):
    ms_content = delete_movie_regex.sub("public void deleteMovie(Long id) {", ms_content)
    # Add redis delete
    ms_content = ms_content.replace("movieRepository.delete(movie);", "movieRepository.delete(movie);\n        redisClient.delete(\"movie:\" + id);")

with open(movie_service_path, "w", encoding="utf-8") as f:
    f.write(ms_content)

print("Done Refactoring Redis")
