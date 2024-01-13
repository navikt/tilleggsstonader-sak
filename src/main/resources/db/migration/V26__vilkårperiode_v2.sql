DROP TABLE vilkar_periode;

CREATE TABLE vilkar_periode
(
    id            UUID    NOT NULL PRIMARY KEY,
    behandling_id UUID    NOT NULL references behandling (id),
    fom           DATE    NOT NULL,
    tom           DATE    NOT NULL,

    type          VARCHAR NOT NULL,
    detaljer      JSON    NOT NULL,
    resultat      VARCHAR NOT NULL
);

CREATE INDEX ON vilkar_periode(behandling_id);