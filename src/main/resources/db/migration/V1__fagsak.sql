create table fagsak_person
(
    id            UUID PRIMARY KEY,
    opprettet_av  varchar      NOT NULL,
    opprettet_tid timestamp(3) NOT NULL
);

create table person_ident
(
    ident            VARCHAR PRIMARY KEY,
    fagsak_person_id UUID         NOT NULL REFERENCES fagsak_person (id),
    opprettet_av     VARCHAR      NOT NULL,
    opprettet_tid    TIMESTAMP(3) NOT NULL,
    endret_av        VARCHAR      NOT NULL,
    endret_tid       TIMESTAMP(3) NOT NULL
);

create table fagsak
(
    id               UUID PRIMARY KEY,
    stonadstype      VARCHAR      NOT NULL,
    opprettet_av     VARCHAR      NOT NULL,
    opprettet_tid    timestamp(3) NOT NULL,
    endret_av        VARCHAR      NOT NULL,
    endret_tid       TIMESTAMP(3) NOT NULL,
    fagsak_person_id UUID         NOT NULL REFERENCES fagsak_person (id),
    CONSTRAINT unique_fagsak_person UNIQUE (fagsak_person_id, stonadstype)
);

create table fagsak_ekstern
(
    id        BIGSERIAL PRIMARY KEY,
    fagsak_id UUID REFERENCES fagsak (id)
);

CREATE INDEX ON person_ident (fagsak_person_id);
CREATE INDEX ON fagsak (fagsak_person_id);
CREATE INDEX ON fagsak_ekstern (fagsak_id);