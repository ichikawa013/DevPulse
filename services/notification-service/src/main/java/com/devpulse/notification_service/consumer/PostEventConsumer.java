package com.devpulse.notification_service.consumer;

import com.devpulse.notification_service.dto.events.PostEvent;
import com.devpulse.notification_service.entities.Notification;
import com.devpulse.notification_service.service.NotificationService;
import io.github.robsonkades.uuidv7.UUIDv7;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostEventConsumer {

    // TODO: recipientEmail is a placeholder (author-as-recipient) since there's no
// followers model yet. Real fix: add a `follows` table (user-service), query
// followers of the post author, loop + create one Notification per follower.
// Not a concern at small scale, but an unbounded fan-out loop — see design
// notes from 2026-08-12 session for batching/celebrity-threshold approach
// if this is ever built for real.
    private final NotificationService notificationService;

    @KafkaListener(topics = "post-events", containerFactory = "postEventListenerFactory")
    public void onPostEvent(PostEvent event) {
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
