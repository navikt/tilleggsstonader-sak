CREATE TABLE kjoreliste_behandling_brev
(
    behandling_id       UUID PRIMARY KEY REFERENCES behandling (id),
    saksbehandler_html  TEXT         NOT NULL,
    pdf                 BYTEA        NOT NULL,
    saksbehandler_ident VARCHAR      NOT NULL,
    opprettet_tid       TIMESTAMP(3) NOT NULL
);
