package com.devpulse.notification_service.consumer;

import com.devpulse.notification_service.dto.events.ReactionEvent;
import com.devpulse.notification_service.entities.Notification;
import com.devpulse.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ReactionEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "reaction-events", containerFactory = "reactionEventListenerFactory")
    public void onReactionEvent(ReactionEvent event) {

        Notification notification = Notification.forReaction(
                event.postId(),
                event.actorEmail(),
                event.postAuthorEmail(),   // recipient — the person whose post got reacted to
                event.reactionType()
        );

        notificationService.sendNotification(notification);
    }
}
