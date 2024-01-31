CREATE TABLE soknad_routing
(
    id            UUID PRIMARY KEY,
    ident         VARCHAR      NOT NULL,
    type          VARCHAR      NOT NULL,
    detaljer      JSON         NOT NULL,
    opprettet_tid TIMESTAMP(3) NOT NULL
);

CREATE UNIQUE INDEX ON soknad_routing(ident, type);