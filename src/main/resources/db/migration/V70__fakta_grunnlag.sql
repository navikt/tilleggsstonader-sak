CREATE TABLE fakta_grunnlag
(
    id            UUID PRIMARY KEY,
    behandling_id UUID         NOT NULL REFERENCES behandling (id),
    data          JSONB        NOT NULL,
    type          TEXT         NOT NULL,
    type_id       TEXT,

    opprettet_av  VARCHAR      NOT NULL,
    opprettet_tid TIMESTAMP(3) NOT NULL,
    endret_av     VARCHAR      NOT NULL,
    endret_tid    TIMESTAMP(3) NOT NULL
);

CREATE INDEX ON fakta_grunnlag (behandling_id);
ALTER TABLE fakta_grunnlag
    ADD CONSTRAINT unique_behandling_type UNIQUE (behandling_id, type, type_id);