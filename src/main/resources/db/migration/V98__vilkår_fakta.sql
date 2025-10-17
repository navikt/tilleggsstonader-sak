create table vilkar_fakta
(
    id            UUID PRIMARY KEY,
    vilkar_id     UUID         NOT NULL UNIQUE REFERENCES vilkar(id),
    type          VARCHAR      NOT NULL,
    data          JSON         NOT NULL,
    opprettet_av  VARCHAR      NOT NULL,
    opprettet_tid timestamp(3) NOT NULL,
    endret_av     VARCHAR      NOT NULL,
    endret_tid    TIMESTAMP(3) NOT NULL
);