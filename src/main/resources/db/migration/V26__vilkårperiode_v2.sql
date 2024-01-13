DROP TABLE vilkar_periode;

CREATE TABLE vilkar_periode
(
    vilkar_id UUID    NOT NULL PRIMARY KEY references vilkar (id),
    fom       DATE    NOT NULL,
    tom       DATE    NOT NULL,

    type      VARCHAR NOT NULL,
    detaljer  JSON    NOT NULL,
    resultat  VARCHAR NOT NULL
);