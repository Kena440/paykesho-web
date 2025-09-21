-- Create roles table
CREATE TABLE IF NOT EXISTS paykesho.role
(
    id
         BIGINT
        NOT
            NULL
        AUTO_INCREMENT,
    is_default
         BOOLEAN,
    description
         VARCHAR(255),
    name VARCHAR(255),
    PRIMARY KEY
        (
         id
            )
);

-- Create users table
CREATE TABLE IF NOT EXISTS paykesho.users
(
    id
                   BIGINT
        NOT
            NULL
        AUTO_INCREMENT,
    account_non_locked
                   BOOLEAN,
    deleted
                   BOOLEAN,
    email
                   VARCHAR(255),
    enabled        BOOLEAN,
    failed_attempt INT,
    first_name     VARCHAR(255),
    gender         VARCHAR(255),
    last_name      VARCHAR(255),
    lock_time      DATETIME,
    password       VARCHAR(255),
    photos         VARCHAR(255),
    photo_path     VARCHAR(255),
    remote_file_id BIGINT,
    PRIMARY KEY
        (
         id
            )
);

-- Create user_roles table
CREATE TABLE IF NOT EXISTS paykesho.user_roles
(
    user_id
        BIGINT
        NOT
            NULL,
    role_id
        BIGINT
        NOT
            NULL,
    PRIMARY
        KEY
        (
         user_id,
         role_id
            ),
    FOREIGN KEY
        (
         user_id
            ) REFERENCES users
        (
         id
            ),
    FOREIGN KEY
        (
         role_id
            ) REFERENCES role
        (
         id
            )
);

-- Create countries table
CREATE TABLE IF NOT EXISTS paykesho.countries
(
    id
         BIGINT
        NOT
            NULL
        AUTO_INCREMENT,
    code
         VARCHAR(255),
    name VARCHAR(255),
    PRIMARY KEY
        (
         id
            )
);

-- Create states table
CREATE TABLE IF NOT EXISTS paykesho.states
(
    id
               BIGINT
        NOT
            NULL
        AUTO_INCREMENT,
    name
               VARCHAR(255),
    country_id BIGINT,
    PRIMARY KEY
        (
         id
            ),
    FOREIGN KEY
        (
         country_id
            ) REFERENCES countries
        (
         id
            )
);

-- Create settings table
CREATE TABLE IF NOT EXISTS paykesho.settings
(
    setting_key
             VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    value    TEXT,
    PRIMARY KEY
        (
         setting_key
            )
);