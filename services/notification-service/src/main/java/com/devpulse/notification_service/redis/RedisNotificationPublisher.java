package com.devpulse.notification_service.redis;

import com.devpulse.notification_service.entities.Notification;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class RedisNotificationPublisher {

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public void publish(Notification notification) {
        try {
            String json =  objectMapper.writeValueAsString(notification);
            redisTemplate.convertAndSend("notifications:push", json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish notification to Redis", e);
        }
    }
}
