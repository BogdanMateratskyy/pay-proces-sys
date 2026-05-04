SET SEARCH_PATH TO payment;

CREATE TABLE IF NOT EXISTS payment_audit_log
(
    id              UUID PRIMARY KEY,
    payment_id      UUID                        NOT NULL,
    previous_status VARCHAR(50),
    new_status      VARCHAR(50)                 NOT NULL,
    transaction_id  VARCHAR(255),
    error_message   TEXT,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT FK__payment_audit_log__payments__id FOREIGN KEY (payment_id)
        REFERENCES payments (id) ON DELETE CASCADE
);
