CREATE TABLE vedtaksbrev
(
    behandling_id         UUID PRIMARY KEY REFERENCES behandling (id),
    saksbehandler_html    VARCHAR      NOT NULL,
    saksbehandlersignatur VARCHAR      NOT NULL,
    besluttersignatur     VARCHAR,
    beslutter_pdf         BYTEA,
    saksbehandler_ident   VARCHAR      NOT NULL,
    beslutter_ident       VARCHAR,
    opprettet_tid         TIMESTAMP(3) NOT NULL,
    besluttet_tid         TIMESTAMP(3)
)