CREATE TABLE kjoreliste
(
    id             UUID PRIMARY KEY,
    journalpost_id VARCHAR      NOT NULL,
    fagsak_id      UUID         NOT NULL REFERENCES fagsak (id),
    dato_mottatt   TIMESTAMP(3) NOT NULL,
    opprettet_av   VARCHAR      NOT NULL,
    opprettet_tid  TIMESTAMP(3) NOT NULL,
    endret_av      VARCHAR      NOT NULL,
    endret_tid     TIMESTAMP(3) NOT NULL,
    data           JSON         NOT NULL
);

CREATE UNIQUE INDEX ON kjoreliste (journalpost_id);
CREATE INDEX ON kjoreliste (fagsak_id);
