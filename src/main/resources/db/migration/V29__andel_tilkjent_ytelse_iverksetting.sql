DROP TABLE andel_tilkjent_ytelse;

CREATE TABLE andel_tilkjent_ytelse
(
    id                     UUID         NOT NULL PRIMARY KEY,
    tilkjent_ytelse_id     UUID         NOT NULL REFERENCES tilkjent_ytelse (id),
    belop                  BIGINT       NOT NULL,
    fom                    DATE         NOT NULL,
    tom                    DATE         NOT NULL,
    satstype               VARCHAR      NOT NULL,
    type                   VARCHAR      NOT NULL,
    version                BIGINT       NOT NULL,
    kilde_behandling_id    UUID         NOT NULL references behandling (id),
    status_iverksetting    VARCHAR      NOT NULL,
    endret_tid             TIMESTAMP(3) NOT NULL,
    iverksetting_id        UUID,
    iverksetting_tidspunkt TIMESTAMP(3)
);

CREATE INDEX ON andel_tilkjent_ytelse (tilkjent_ytelse_id);