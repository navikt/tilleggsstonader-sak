CREATE TABLE soknad
(
    id             UUID PRIMARY KEY,
    journalpost_id VARCHAR      NOT NULL,
    dato_mottatt   TIMESTAMP(3) NOT NULL,
    opprettet_av   VARCHAR      NOT NULL,
    opprettet_tid  TIMESTAMP(3) NOT NULL,
    endret_av      VARCHAR      NOT NULL,
    endret_tid     TIMESTAMP(3) NOT NULL
);

CREATE TABLE soknad_barn
(
    id            UUID PRIMARY KEY,
    fodselsnummer VARCHAR NOT NULL,
    navn          VARCHAR NOT NULL,
    soknad_id     UUID REFERENCES soknad (id)
);
CREATE INDEX ON soknad_barn (soknad_id);

CREATE table soknad_behandling
(
    behandling_id UUID PRIMARY KEY,
    soknad_id     UUID REFERENCES soknad (id),
    opprettet_av  VARCHAR      NOT NULL,
    opprettet_tid TIMESTAMP(3) NOT NULL,
    endret_av     VARCHAR      NOT NULL,
    endret_tid    TIMESTAMP(3) NOT NULL
);
CREATE INDEX ON soknad_behandling (soknad_id);
