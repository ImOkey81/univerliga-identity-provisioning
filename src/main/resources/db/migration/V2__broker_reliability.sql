ALTER TABLE processed_events
    ADD COLUMN IF NOT EXISTS fail_count INT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_identity_links_status ON identity_links (status);
CREATE INDEX IF NOT EXISTS idx_identity_links_person_id ON identity_links (person_id);
