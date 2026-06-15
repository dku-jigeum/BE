CREATE TABLE IF NOT EXISTS user_bookmark (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(100)  NOT NULL,
    event_id    VARCHAR(100)  NOT NULL,
    issue_type  VARCHAR(20)   NOT NULL,
    title       VARCHAR(300)  NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_bookmark_user_event UNIQUE (user_id, event_id)
);

CREATE INDEX IF NOT EXISTS idx_user_bookmark_user_id ON user_bookmark (user_id);
