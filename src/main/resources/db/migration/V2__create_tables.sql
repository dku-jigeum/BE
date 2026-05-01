CREATE TABLE IF NOT EXISTS bill (
    id               BIGSERIAL PRIMARY KEY,
    bill_no          VARCHAR(50)  UNIQUE NOT NULL,
    title            VARCHAR(500) NOT NULL,
    content          TEXT,
    deadline         DATE,
    embedding_vector vector(1536),
    view_count       INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS petition (
    id                BIGSERIAL PRIMARY KEY,
    petition_no       VARCHAR(50)  UNIQUE NOT NULL,
    title             VARCHAR(500) NOT NULL,
    content           TEXT,
    deadline          DATE,
    participant_count INTEGER      NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS legislation_notice (
    id         BIGSERIAL PRIMARY KEY,
    notice_no  VARCHAR(50)  UNIQUE NOT NULL,
    title      VARCHAR(500) NOT NULL,
    content    TEXT,
    deadline   DATE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_profile (
    id         BIGSERIAL PRIMARY KEY,
    user_id    VARCHAR(100) UNIQUE NOT NULL,
    fcm_token  VARCHAR(500),
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_interest_tag (
    user_profile_id BIGINT      NOT NULL REFERENCES user_profile (id) ON DELETE CASCADE,
    tag             VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_profile_id, tag)
);
