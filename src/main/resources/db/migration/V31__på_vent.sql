CREATE TABLE sett_pa_vent
(
    id            UUID PRIMARY KEY,
    behandling_id UUID         NOT NULL references behandling (id),
    oppgave_id    BIGINT       NOT NULL,
    aktiv         BOOLEAN      NOT NULL,
    arsaker       VARCHAR[]    NOT NULL,

    opprettet_av  VARCHAR      NOT NULL,
    opprettet_tid TIMESTAMP(3) NOT NULL,
    endret_av     VARCHAR      NOT NULL,
    endret_tid    TIMESTAMP(3) NOT NULL
);

CREATE INDEX ON sett_pa_vent (behandling_id);
CREATE UNIQUE INDEX ON sett_pa_vent (behandling_id) WHERE aktiv = true;