CREATE TABLE brevmottaker
(
    id                    UUID PRIMARY KEY,
    behandling_id         UUID         NOT NULL REFERENCES behandling (id),
    ident                 VARCHAR      NOT NULL,
    mottaker_rolle        VARCHAR      NOT NULL,
    mottaker_type         VARCHAR      NOT NULL,
    navn_hos_organisasjon VARCHAR,

    journalpost_id        VARCHAR,
    bestilling_id         VARCHAR,

    CONSTRAINT unik_mottaker UNIQUE (behandling_id, ident),

    opprettet_av          VARCHAR      NOT NULL,
    opprettet_tid         TIMESTAMP(3) NOT NULL,
    endret_av             VARCHAR      NOT NULL,
    endret_tid            TIMESTAMP(3) NOT NULL
);

CREATE INDEX ON brevmottaker (behandling_id);