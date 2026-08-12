package com.devpulse.notification_service.resolver;

import com.devpulse.notification_service.entities.Notification;
import com.devpulse.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class NotificationQueryResolver {

    private final NotificationRepository notificationRepository;

    @QueryMapping
    public List<Notification> notifications(@Argument Integer limit, @ContextValue("X-User-Id") String recipientEmail) {
        int pageSize = (limit != null) ? limit : 20;
        return notificationRepository
                .findAllByRecipientEmailOrderByCreatedAtDescIdDesc(recipientEmail, PageRequest.of(0, pageSize))
                .getContent();
    }

    @SchemaMapping(typeName = "Notification", field = "metadata")
    public String metadata(Notification notification) {
        return notification.getMetadata() != null ? notification.getMetadata().toString() : null;
    }
}