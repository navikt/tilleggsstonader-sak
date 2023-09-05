create table fagsak
(
    id            UUID PRIMARY KEY NOT NULL,

    stonadstype   VARCHAR          NOT NULL,
    opprettet_av  VARCHAR          NOT NULL,
    opprettet_tid timestamp(3)     NOT NULL,
    endret_av     VARCHAR          NOT NULL,
    endret_tid    TIMESTAMP(3)     NOT NULL
    --#fagsak_person_id uuid                                NOT NULL references fagsak_person,
    -- constraint fagsak_person_unique unique (fagsak_person_id, stonadstype)
);

create table fagsak_ekstern
(
    id        BIGSERIAL PRIMARY KEY,
    fagsak_id UUID REFERENCES fagsak(id)
);

CREATE INDEX ON fagsak_ekstern (fagsak_id);