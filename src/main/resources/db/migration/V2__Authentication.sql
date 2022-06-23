CREATE TABLE JWT
(
    jwt_id           VARCHAR PRIMARY KEY,
    jwt          VARCHAR,
    account_id   BIGINT REFERENCES account (account_id),
    expiry       TIMESTAMP,
    last_touched TIMESTAMP
);
