CREATE TABLE iverksetting
(
    id            UUID PRIMARY KEY,
    fagsak_id     UUID         NOT NULL REFERENCES fagsak (id),
    behandling_id UUID         NOT NULL REFERENCES behandling (id),
    aktiv         BOOLEAN      NOT NULL,
    opprettet     TIMESTAMP(3) NOT NULL,
    version       INTEGER      NOT NULL,
    status        VARCHAR      NOT NULL
);

CREATE TABLE iverksetting_historikk
(
    id        UUID REFERENCES iverksetting (id),
    opprettet TIMESTAMP(3) NOT NULL,
    status    VARCHAR      NOT NULL
);

CREATE UNIQUE INDEX ON iverksetting (fagsak_id) WHERE aktiv = true;
