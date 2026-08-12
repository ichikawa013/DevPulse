package com.devpulse.feed_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reactions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Reaction implements Persistable<UUID> {

    @Id
    private UUID id;

    @JoinColumn(name = "post_id", nullable = false)
    private UUID postId;

    @Column(nullable = false)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReactionType reactionType;

    @CreatedDate
    @Column(nullable = false)
    private Instant createdAt;

    public Reaction(UUID id, UUID postId, String actorEmail, ReactionType reactionType) {
        this.id = id;
        this.postId = postId;
        this.actorEmail = actorEmail;
        this.reactionType = reactionType;
        this.isNew = true;
    }

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
