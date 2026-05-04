SET SEARCH_PATH TO payment;

CREATE TABLE IF NOT EXISTS payment_statuses
(
    id             UUID PRIMARY KEY,
    payment_id     UUID         NOT NULL,
    status         VARCHAR(50)  NOT NULL,
    transaction_id VARCHAR(255),
    error_message  TEXT,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT FK__payment_statuses__payments__id FOREIGN KEY (payment_id)
        REFERENCES payments (id) ON DELETE CASCADE
);

ALTER TABLE payments
    ADD COLUMN status_id UUID;

ALTER TABLE payments
    ADD CONSTRAINT FK__payments__payment_statuses__id
        FOREIGN KEY (status_id) REFERENCES payment_statuses (id);

ALTER TABLE payments
    DROP COLUMN status;
