CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    event_id   UUID                     NOT NULL UNIQUE,
    order_id   BIGINT                   NOT NULL,
    event_type VARCHAR(40)              NOT NULL,
    recipient  VARCHAR(150)             NOT NULL,
    subject    VARCHAR(200)             NOT NULL,
    message    TEXT                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient);
CREATE INDEX idx_notifications_order_id ON notifications (order_id);
