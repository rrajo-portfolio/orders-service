CREATE TABLE orders (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    user_full_name VARCHAR(160),
    user_email VARCHAR(160),
    status VARCHAR(16) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    notes VARCHAR(300),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    version BIGINT,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE order_items (
    id BINARY(16) NOT NULL,
    order_id BINARY(16),
    product_id BINARY(16) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    title VARCHAR(120),
    PRIMARY KEY (id),
    CONSTRAINT FK_orders_items FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;
