CREATE TABLE reactions (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(id),
    actor_email VARCHAR(255) NOT NULL,
    reaction_type TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_reaction_once_per_user UNIQUE (post_id, actor_email)
);
