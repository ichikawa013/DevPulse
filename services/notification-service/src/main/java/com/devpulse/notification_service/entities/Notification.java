package com.devpulse.notification_service.entities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.robsonkades.uuidv7.UUIDv7;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@Table(name = "notifications")
@Getter
@Setter
public class Notification implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID sourcePostId;

    @Column(nullable = false)
    private String actorEmail;

    @Column(nullable = false)
    private String recipientEmail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode metadata;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Event eventType;

    @Column(nullable = false)
    private Boolean read = false;

    @CreatedDate
    @Column(nullable = false)
    private Instant createdAt;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Transient
    private boolean isNew = true;

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public Notification(UUID id, UUID sourcePostId, String actorEmail,
                        String recipientEmail, JsonNode metadata, Event eventType) {
        this.id = id;
        this.sourcePostId = sourcePostId;
        this.actorEmail = actorEmail;
        this.recipientEmail = recipientEmail;
        this.metadata = metadata;
        this.eventType = eventType;
        this.isNew = true;
    }

    public static Notification forPost(UUID postId, String authorEmail, String recipientEmail, String contentPreview) {
        ObjectNode metadata = JsonNodeFactory.instance.objectNode();
        metadata.put("contentPreview", contentPreview);
        return new Notification(
                UUIDv7.randomUUID(),
                postId,
                authorEmail,
                recipientEmail,
                metadata,
                Event.TYPE_POST
        );
    }

    public static Notification forReaction(UUID postId, String actorEmail, String recipientEmail, String reactionType) {
        ObjectNode metadata = JsonNodeFactory.instance.objectNode();
        metadata.put("reactionType", reactionType);
        return new Notification(
                UUIDv7.randomUUID(),
                postId,
                actorEmail,
                recipientEmail,
                metadata,
                Event.TYPE_REACTION
        );
    }
}
