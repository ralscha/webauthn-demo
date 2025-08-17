CREATE TABLE app_user (
    id             BIGSERIAL PRIMARY KEY, 
    username       VARCHAR(255) NOT NULL 
);

CREATE TABLE credentials (
    id                           BYTEA NOT NULL PRIMARY KEY,
    app_user_id                  BIGINT NOT NULL,
    public_key                   BYTEA NOT NULL,
    signature_count              BIGINT NOT NULL,
    authenticator_transports     VARCHAR(10000),
    uv_initialized               BOOLEAN,
    backup_eligible              BOOLEAN       NOT NULL,
    public_key_credential_type   VARCHAR(100),
    backup_state                 BOOLEAN       NOT NULL,
    attestation_object           BYTEA,
    attestation_client_data_json BYTEA,
    created                      timestamp,
    last_used                    timestamp,
    label                        varchar(1000) NOT NULL,
    FOREIGN KEY (app_user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_credentials_app_user_id ON credentials(app_user_id);
