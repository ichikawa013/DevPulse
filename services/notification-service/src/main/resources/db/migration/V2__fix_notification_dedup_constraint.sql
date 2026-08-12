-- V2__fix_notification_dedup_constraint.sql
ALTER TABLE notifications DROP CONSTRAINT uq_notification_dedup;
ALTER TABLE notifications ADD CONSTRAINT uq_notification_dedup
    UNIQUE (source_post_id, event_type, recipient_email, actor_email);