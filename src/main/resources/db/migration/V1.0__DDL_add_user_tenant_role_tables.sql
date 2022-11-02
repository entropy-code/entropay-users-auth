CREATE TABLE "user" (
    id          UUID    NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    deleted     BOOLEAN NOT NULL,
    username    VARCHAR(255),
    first_name  VARCHAR(255),
    last_name   VARCHAR(255),
    email       VARCHAR(255),
    CONSTRAINT pk_user PRIMARY KEY (id)
)
;

INSERT INTO "user"
SELECT
    '360e99a0-2619-469d-b88b-6d6355e3d8fa', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE, 'admin', 'entropay', 'admin'
    , 'admin@entropay.com', null
;

CREATE TABLE tenant (
    id           UUID    NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    deleted      BOOLEAN NOT NULL,
    name         VARCHAR(255),
    display_name VARCHAR(255),
    CONSTRAINT pk_tenant PRIMARY KEY (id)
)
;

INSERT INTO tenant
SELECT '8a6c45de-a257-4f07-95e1-9e163f71f2f1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE, 'entropy', 'Entropy Team'
;

CREATE TABLE role (
    id          UUID    NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    deleted     BOOLEAN NOT NULL,
    role_name   VARCHAR(255),
    CONSTRAINT pk_role PRIMARY KEY (id)
)
;

INSERT INTO role
SELECT 'bc477234-c7bf-4ca3-9ade-576d40b7a5db', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE, 'ADMIN'
;

CREATE TABLE user_tenant (
    id          UUID    NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    deleted     BOOLEAN NOT NULL,
    user_id     UUID,
    tenant_id   UUID,
    role_id     UUID,
    CONSTRAINT pk_user_tenant PRIMARY KEY (id)
)
;

ALTER TABLE user_tenant
    ADD CONSTRAINT FK_USER_TENANT_ON_ROLE FOREIGN KEY (role_id) REFERENCES role(id)
;

ALTER TABLE user_tenant
    ADD CONSTRAINT FK_USER_TENANT_ON_TENANT FOREIGN KEY (tenant_id) REFERENCES tenant(id)
;

ALTER TABLE user_tenant
    ADD CONSTRAINT FK_USER_TENANT_ON_USER FOREIGN KEY (user_id) REFERENCES "user"(id)
;

INSERT INTO user_tenant
SELECT '459f9cb0-c673-4d51-8cfb-d6fc328dbf7d', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE, '360e99a0-2619-469d-b88b-6d6355e3d8fa', '8a6c45de-a257-4f07-95e1-9e163f71f2f1', 'bc477234-c7bf-4ca3-9ade-576d40b7a5db'
;
