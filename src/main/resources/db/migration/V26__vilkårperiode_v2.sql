DROP TABLE vilkar_periode;

CREATE TABLE vilkar_periode
(
    id            UUID         NOT NULL PRIMARY KEY,
    behandling_id UUID         NOT NULL references behandling (id),
    fom           DATE         NOT NULL,
    tom           DATE         NOT NULL,

    type          VARCHAR      NOT NULL,
    detaljer      JSON         NOT NULL,
    begrunnelse   VARCHAR,
    resultat      VARCHAR      NOT NULL,

    kilde         VARCHAR      NOT NULL,
    opprettet_av  VARCHAR      NOT NULL,
    opprettet_tid TIMESTAMP(3) NOT NULL,
    endret_av     VARCHAR      NOT NULL,
    endret_tid    TIMESTAMP(3) NOT NULL
);

CREATE INDEX ON vilkar_periode(behandling_id);