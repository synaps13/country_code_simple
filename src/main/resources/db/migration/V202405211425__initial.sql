CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE country_code
(
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone_code INTEGER NOT NULL,
    country VARCHAR(100) NOT NULL
);