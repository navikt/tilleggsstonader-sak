CREATE TABLE behandling_barn
(
    id             UUID PRIMARY KEY,
    behandling_id  UUID         NOT NULL references behandling (id),
    person_ident   VARCHAR      NOT NULL,
    soknad_barn_id UUID,
    navn           VARCHAR      NOT NULL,
    opprettet_av   VARCHAR      NOT NULL,
    endret_av      VARCHAR      NOT NULL,
    opprettet_tid  TIMESTAMP(3) NOT NULL,
    endret_tid     TIMESTAMP(3) NOT NULL
);

CREATE INDEX behandling_barn_behandling_id_idx ON behandling_barn (behandling_id);

CREATE UNIQUE INDEX ON behandling_barn (behandling_id, person_ident)