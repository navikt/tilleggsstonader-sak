CREATE TABLE journalpost_resultat (
    id             UUID PRIMARY KEY,
    behandling_id  UUID         NOT NULL REFERENCES behandling (id),
    mottaker_id    VARCHAR      NOT NULL,
    journalpost_id VARCHAR      NOT NULL,
    bestilling_id  VARCHAR,
    CONSTRAINT journalpostresultat_mottaker UNIQUE (behandling_id, mottaker_id),

    opprettet_av   VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid  TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av      VARCHAR      NOT NULL,
    endret_tid     TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP
);