package com.devpulse.notification_service.service;

import com.devpulse.notification_service.entities.Notification;
import com.devpulse.notification_service.redis.RedisNotificationPublisher;
import com.devpulse.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RedisNotificationPublisher redisNotificationPublisher;

    public void sendNotification(Notification notification) {
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification pushed for {}", savedNotification.getActorEmail());
        redisNotificationPublisher.publish(savedNotification);
    }
}
