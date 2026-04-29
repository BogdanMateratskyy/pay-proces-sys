CREATE TABLE IF NOT EXISTS payments
(
    id                UUID PRIMARY KEY,
    amount            DECIMAL(19, 4) NOT NULL,
    currency          VARCHAR(3)     NOT NULL,
    recipient_account VARCHAR(255)   NOT NULL,
    sender_account    VARCHAR(255)   NOT NULL,
    status            VARCHAR(50)    NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
