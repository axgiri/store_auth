package com.github.oldlabauth.repository;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.github.oldlabauth.dto.MessageChannelEnum;
import com.github.oldlabauth.entity.OtpType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OtpRedisRepository {

    private static final String KEY_PREFIX = "otp";
    private static final String KEY_SEPARATOR = ":";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.otp-ttl-minutes}")
    private int otpTtlMinutes;

    public void save(MessageChannelEnum channel, String contact, OtpType otpType, int otp) {
        String key = buildKey(channel, contact, otpType);
        log.debug("Saving OTP for key: {}", key);
        
        redisTemplate.opsForValue().set(
            key, 
            String.valueOf(otp), 
            Duration.ofMinutes(otpTtlMinutes)
        );
    }

    public Optional<Integer> find(MessageChannelEnum channel, String contact, OtpType otpType) {
        String key = buildKey(channel, contact, otpType);
        String value = redisTemplate.opsForValue().get(key);
        
        if (value == null) {
            log.debug("No OTP found for key: {}", key);
            return Optional.empty();
        }
        
        return Optional.of(Integer.parseInt(value));
    }

    public boolean delete(MessageChannelEnum channel, String contact, OtpType otpType) {
        String key = buildKey(channel, contact, otpType);
        Boolean deleted = redisTemplate.delete(key);
        log.debug("Deleted OTP for key: {}, result: {}", key, deleted);
        return Boolean.TRUE.equals(deleted);
    }

    public boolean exists(MessageChannelEnum channel, String contact, OtpType otpType) {
        String key = buildKey(channel, contact, otpType);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public long getRemainingTtlSeconds(MessageChannelEnum channel, String contact, OtpType otpType) {
        String key = buildKey(channel, contact, otpType);
        Long ttl = redisTemplate.getExpire(key);
        return ttl != null ? ttl : -1;
    }

    private String buildKey(MessageChannelEnum channel, String contact, OtpType otpType) {
        return String.join(KEY_SEPARATOR, 
            KEY_PREFIX, 
            otpType.name(), 
            channel.name(), 
            contact.toLowerCase()
        );
    }
}
