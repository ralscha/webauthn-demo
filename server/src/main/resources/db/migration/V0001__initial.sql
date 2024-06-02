CREATE TABLE app_user (
    id                      BIGINT NOT NULL AUTO_INCREMENT,
    username                VARCHAR(255) NOT NULL, 
    recovery_token          BINARY(16) NULL,
    registration_start      TIMESTAMP NULL,
    PRIMARY KEY(id),
    UNIQUE(username)    
);

CREATE TABLE credentials (
    id              VARBINARY(128) NOT NULL,
    app_user_id     BIGINT         NOT NULL,
    webauthn_user_id VARBINARY(64) NOT NULL,
    count           BIGINT     NOT NULL,
    public_key_cose VARBINARY(500) NOT NULL,
    transports      VARCHAR(255),
    UNIQUE(webauthn_user_id),
    PRIMARY KEY(id, app_user_id),
    FOREIGN KEY (app_user_id) REFERENCES app_user(id) ON DELETE CASCADE
);