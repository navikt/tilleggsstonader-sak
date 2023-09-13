CREATE TABLE simuleringsresultat
(
    behandling_id UUID PRIMARY KEY REFERENCES behandling (id),
    opprettet_av  VARCHAR      NOT NULL,
    opprettet_tid TIMESTAMP(3) NOT NULL,
    endret_av     VARCHAR      NOT NULL,
    endret_tid    TIMESTAMP(3) NOT NULL,
    data          JSON         NOT NULL
);
