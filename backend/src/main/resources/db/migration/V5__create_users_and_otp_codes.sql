-- Users and phone login codes.
-- A user signs in with email and password, phone OTP, or Google. At least one identity is required.
-- Emails are stored in lower case by the app.

CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(255),
    phone          VARCHAR(16),
    password_hash  VARCHAR(100),
    google_subject VARCHAR(255),
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('USER', 'THEATRE_ADMIN', 'ADMIN')),
    created_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone),
    CONSTRAINT uq_users_google_subject UNIQUE (google_subject),
    CONSTRAINT chk_users_identity CHECK (email IS NOT NULL OR phone IS NOT NULL OR google_subject IS NOT NULL)
);

-- Only a hash of each code is stored. The newest code per phone is the one that counts.
CREATE TABLE otp_codes (
    id         BIGSERIAL PRIMARY KEY,
    phone      VARCHAR(16)  NOT NULL,
    code_hash  VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    attempts   INTEGER      NOT NULL CHECK (attempts >= 0),
    used       BOOLEAN      NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_otp_codes_phone_created ON otp_codes (phone, created_at DESC);
