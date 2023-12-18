CREATE TABLE stonadsperiode
(
    id            UUID    NOT NULL PRIMARY KEY,
    behandling_id UUID    NOT NULL REFERENCES behandling (id),
    fom           DATE    NOT NULL,
    tom           DATE    NOT NULL,
    malgruppe     VARCHAR NOT NULL,
    aktivitet     VARCHAR NOT NULL
);

CREATE INDEX ON stonadsperiode (behandling_id);