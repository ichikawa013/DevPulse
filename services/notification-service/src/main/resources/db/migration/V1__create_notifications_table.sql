CREATE TABLE notifications(
    id UUID PRIMARY KEY,
    source_post_id UUID NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    actor_email VARCHAR(255) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}',
    event_type TEXT NOT NULL,
    read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_notification_dedup UNIQUE (source_post_id, event_type, recipient_email)
);

CREATE INDEX idx_notifications_recipient_created ON notifications (recipient_email, created_at DESC, id DESC);

--CREATE INDEX idx_notifications_metadata
--    ON notifications USING GIN (metadata);

--metadata carries whatever's specific to the type: for REACTION that might be {"reaction_type": "like"};
-- if you add a comment-notification later,{"comment_id": "...", "preview": "..."}.
--Fixed columns don't grow every time you add a feature; this column does the growing.