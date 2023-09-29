CREATE TABLE tilkjent_ytelse
(
    id            UUID PRIMARY KEY,
    opprettet_av  VARCHAR      NOT NULL,
    opprettet_tid TIMESTAMP(3) NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3),
    behandling_id UUID         NOT NULL REFERENCES behandling (id),
    startdato     DATE

);

CREATE INDEX ON tilkjent_ytelse (behandling_id);

create table andel_tilkjent_ytelse
(
    tilkjent_ytelse     UUID   NOT NULL REFERENCES tilkjent_ytelse (id),
    tilkjent_ytelse_key BIGINT NOT NULL,
    stonad_fom          DATE   NOT NULL,
    stonad_tom          DATE   NOT NULL,
    belop               BIGINT NOT NULL,
    kilde_behandling_id UUID   NOT NULL REFERENCES behandling (id)
);

CREATE INDEX ON andel_tilkjent_ytelse (tilkjent_ytelse);

