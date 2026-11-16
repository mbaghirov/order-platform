CREATE TABLE products (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(150)             NOT NULL,
    description    TEXT,
    price          NUMERIC(12, 2)           NOT NULL CHECK (price > 0),
    stock_quantity INTEGER                  NOT NULL CHECK (stock_quantity >= 0),
    version        BIGINT                   NOT NULL DEFAULT 0,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    customer_email  VARCHAR(150)             NOT NULL,
    status          VARCHAR(20)              NOT NULL,
    subtotal        NUMERIC(12, 2)           NOT NULL,
    discount_amount NUMERIC(12, 2)           NOT NULL,
    total_amount    NUMERIC(12, 2)           NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id   BIGINT         NOT NULL REFERENCES products (id),
    product_name VARCHAR(150)   NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL,
    quantity     INTEGER        NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_orders_customer_email ON orders (customer_email);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);
