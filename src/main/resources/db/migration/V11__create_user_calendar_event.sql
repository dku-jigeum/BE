CREATE TABLE IF NOT EXISTS user_calendar_event (
    id              BIGSERIAL PRIMARY KEY,
    user_id         VARCHAR(100)  NOT NULL,
    event_id        VARCHAR(100)  NOT NULL,
    issue_type      VARCHAR(20)   NOT NULL,
    calendar_title  VARCHAR(300)  NOT NULL,
    calendar_date   DATE          NOT NULL,
    reminder        VARCHAR(20),
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_event UNIQUE (user_id, event_id)
);

CREATE INDEX IF NOT EXISTS idx_uce_user_id ON user_calendar_event (user_id);
