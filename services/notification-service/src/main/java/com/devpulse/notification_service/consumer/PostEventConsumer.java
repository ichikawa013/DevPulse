package com.devpulse.notification_service.consumer;

import com.devpulse.notification_service.dto.events.PostEvent;
import com.devpulse.notification_service.entities.Notification;
import com.devpulse.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "post-events",
            containerFactory = "postEventListenerFactory"
    )
    public void onPostEvent(
            PostEvent event,
            ConsumerRecord<String, Object> record
    ) {

        log.info("Post Event occurred:{}", event);

        Notification notification = Notification.forPost(
                event.postId(),
                event.authorEmail(),
                event.authorEmail(),
                event.content()
        );

        notificationService.sendNotification(notification);
    }
}