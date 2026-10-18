-- Batch 4a: friends, friends activity, gift tickets, group polls. Only adds.

-- Users can hide their movie activity from friends.
ALTER TABLE users ADD COLUMN share_activity BOOLEAN NOT NULL DEFAULT TRUE;

-- One row per pair of users, whoever asked first. A declined or removed pair is reused for a new request.
CREATE TABLE friendships (
    id           BIGSERIAL PRIMARY KEY,
    requester_id BIGINT      NOT NULL REFERENCES users (id),
    addressee_id BIGINT      NOT NULL REFERENCES users (id),
    status       VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'REMOVED')),
    created_at   TIMESTAMPTZ NOT NULL,
    responded_at TIMESTAMPTZ,
    CONSTRAINT chk_friendships_not_self CHECK (requester_id <> addressee_id)
);

CREATE UNIQUE INDEX uq_friendships_pair
    ON friendships (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id));
CREATE INDEX idx_friendships_addressee ON friendships (addressee_id, status);
CREATE INDEX idx_friendships_requester ON friendships (requester_id, status);

-- Gift: who gave the ticket.
ALTER TABLE bookings ADD COLUMN gifted_from_user_id BIGINT REFERENCES users (id);

-- Group polls: friends vote on which show to watch.
CREATE TABLE polls (
    id              BIGSERIAL PRIMARY KEY,
    creator_id      BIGINT       NOT NULL REFERENCES users (id),
    title           VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    winning_show_id BIGINT REFERENCES shows (id),
    created_at      TIMESTAMPTZ  NOT NULL,
    closed_at       TIMESTAMPTZ
);

CREATE TABLE poll_options (
    id      BIGSERIAL PRIMARY KEY,
    poll_id BIGINT NOT NULL REFERENCES polls (id),
    show_id BIGINT NOT NULL REFERENCES shows (id),
    CONSTRAINT uq_poll_options UNIQUE (poll_id, show_id)
);

CREATE TABLE poll_members (
    id      BIGSERIAL PRIMARY KEY,
    poll_id BIGINT NOT NULL REFERENCES polls (id),
    user_id BIGINT NOT NULL REFERENCES users (id),
    CONSTRAINT uq_poll_members UNIQUE (poll_id, user_id)
);

CREATE INDEX idx_poll_members_user ON poll_members (user_id);

-- One vote per member per poll. Changing a vote updates the row.
CREATE TABLE poll_votes (
    id         BIGSERIAL PRIMARY KEY,
    poll_id    BIGINT      NOT NULL REFERENCES polls (id),
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    show_id    BIGINT      NOT NULL REFERENCES shows (id),
    voted_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_poll_votes UNIQUE (poll_id, user_id)
);
