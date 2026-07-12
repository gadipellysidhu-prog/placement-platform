-- Master Skills Catalog: extend skills with catalog metadata and add the alias table.
-- All skill columns are nullable or defaulted so existing rows remain valid.

ALTER TABLE skills
    ADD COLUMN description      TEXT,
    ADD COLUMN parent_category  VARCHAR(50),
    ADD COLUMN subcategory      VARCHAR(50),
    ADD COLUMN popularity_score INT           NOT NULL DEFAULT 0,
    ADD COLUMN industry_tags    VARCHAR(255),
    ADD COLUMN active           BOOLEAN       NOT NULL DEFAULT TRUE,
    ADD COLUMN created_source   VARCHAR(20)   NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN ai_confidence    NUMERIC(5,2);

-- Alias database: every skill can be referenced by alternative names/abbreviations.
-- alias_normalized (lower/trim/space-collapsed) is globally unique so an alias can
-- never point at two skills, and duplicate detection is a single indexed lookup.
CREATE TABLE skill_aliases (
    id               UUID PRIMARY KEY,
    version          BIGINT       NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    skill_id         UUID         NOT NULL REFERENCES skills (id) ON DELETE CASCADE,
    alias            VARCHAR(100) NOT NULL,
    alias_normalized VARCHAR(100) NOT NULL,
    CONSTRAINT uq_skill_aliases_normalized UNIQUE (alias_normalized)
);

CREATE INDEX idx_skill_aliases_skill_id ON skill_aliases (skill_id);

-- Trigram index so fuzzy matching can also search aliases (pg_trgm enabled in V1_0_0).
CREATE INDEX idx_skill_aliases_alias_trgm ON skill_aliases USING gin (alias gin_trgm_ops);
