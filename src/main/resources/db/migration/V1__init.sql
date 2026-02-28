CREATE TABLE identity_links (
    person_id VARCHAR(128) PRIMARY KEY,
    keycloak_user_id VARCHAR(64),
    username VARCHAR(128) NOT NULL,
    email VARCHAR(256) NOT NULL,
    status VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    display_name VARCHAR(256),
    department_id VARCHAR(128),
    team_id VARCHAR(128),
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uq_identity_links_username ON identity_links (username);
CREATE UNIQUE INDEX uq_identity_links_email ON identity_links (email);

CREATE TABLE identity_link_roles (
    person_id VARCHAR(128) NOT NULL REFERENCES identity_links(person_id) ON DELETE CASCADE,
    role_name VARCHAR(128) NOT NULL,
    PRIMARY KEY (person_id, role_name)
);

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    error TEXT,
    attempt_count INT NOT NULL DEFAULT 0,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_processed_events_status ON processed_events(status);
