package com.devpulse.notification_service.redis;

import com.devpulse.notification_service.entities.Notification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class RedisNotificationSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public void handleMessage(String json) throws JsonProcessingException {
        Notification notification = objectMapper.readValue(json, Notification.class);

        messagingTemplate.convertAndSendToUser(
            notification.getRecipientEmail(), "/queue/notifications", notification
        );
    }
}
