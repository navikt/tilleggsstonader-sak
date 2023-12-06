CREATE TABLE vilkar_periode
(
    vilkar_id UUID    NOT NULL PRIMARY KEY REFERENCES vilkar (id),
    fom       DATE    NOT NULL,
    tom       DATE    NOT NULL,
    type      VARCHAR NOT NULL
);