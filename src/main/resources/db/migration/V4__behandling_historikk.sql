CREATE TABLE behandlingshistorikk
(
    id                UUID PRIMARY KEY,
    behandling_id     UUID         NOT NULL REFERENCES behandling (id),
    steg              VARCHAR      NOT NULL,
    opprettet_av_navn VARCHAR      NOT NULL,
    opprettet_av      VARCHAR      NOT NULL,
    endret_tid        TIMESTAMP(3) NOT NULL,
    utfall            VARCHAR,
    metadata          JSON
);

CREATE INDEX ON behandlingshistorikk (behandling_id);

