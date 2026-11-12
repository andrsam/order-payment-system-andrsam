-- changeset init-orders-:001

CREATE TABLE IF NOT EXISTS orders
(
    id                  UUID PRIMARY KEY DEFAULT uuidv7(),
    customer_id         numeric(38, 2),
    address             TEXT,
    client_estimate   numeric(38, 2),
    authorized_amount numeric(38, 2),
    captured_amount   numeric(38, 2),
    final_amount      numeric(38, 2),
    payment_status    varchar(255)
);