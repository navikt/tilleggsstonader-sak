CREATE TABLE mellomlagret_brev (
    behandling_id UUID PRIMARY KEY REFERENCES behandling (id),
    brevverdier   VARCHAR      NOT NULL,
    brevmal       VARCHAR      NOT NULL,

    opprettet_av  VARCHAR      NOT NULL,
    opprettet_tid TIMESTAMP(3) NOT NULL,
    endret_av     VARCHAR      NOT NULL,
    endret_tid    TIMESTAMP(3) NOT NULL
);

CREATE TABLE mellomlagret_frittstaende_brev (
    id            UUID PRIMARY KEY,
    fagsak_id     UUID         NOT NULL REFERENCES fagsak (id),
    brevverdier   VARCHAR      NOT NULL,
    brevmal       VARCHAR      NOT NULL,

    opprettet_av  VARCHAR      NOT NULL,
    opprettet_tid TIMESTAMP(3) NOT NULL,
    endret_av     VARCHAR      NOT NULL,
    endret_tid    TIMESTAMP(3) NOT NULL
);


