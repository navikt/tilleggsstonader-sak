CREATE TABLE vedtak_tilsyn_barn
(
    behandling_id      UUID         NOT NULL PRIMARY KEY REFERENCES behandling (id),
    type               VARCHAR      NOT NULL,
    vedtak             JSON         NOT NULL,
    beregningsresultat JSON,

    opprettet_av       VARCHAR      NOT NULL,
    opprettet_tid      TIMESTAMP(3) NOT NULL,
    endret_av          VARCHAR      NOT NULL,
    endret_tid         TIMESTAMP(3) NOT NULL
);

