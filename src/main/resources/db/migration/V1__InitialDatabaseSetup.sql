CREATE TABLE account
(
    account_id    BIGSERIAL PRIMARY KEY,
    nickname      VARCHAR(20),
    email         VARCHAR(30),
    phone_number  VARCHAR(30),
    password_hash VARCHAR,
    role          VARCHAR(30)
);
CREATE TABLE profile
(
    profile_id BIGSERIAL PRIMARY KEY,
    account_id BIGINT REFERENCES account (account_id),
    first_name VARCHAR(30),
    surname    VARCHAR(30),
    gender     VARCHAR(30)
);
CREATE TABLE property
(
    property_id BIGSERIAL PRIMARY KEY,
    account_id  BIGINT REFERENCES account (account_id),
    token_hash  VARCHAR,
    description TEXT
);
