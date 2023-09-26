CREATE table totrinnsstatus
(
    id             UUID PRIMARY KEY,
    behandling_id  UUID NOT NULL REFERENCES behandling (id),
    opprettet_av   VARCHAR      NOT NULL,
    opprettet_tid  TIMESTAMP(3) NOT NULL,
    endret_av      VARCHAR      NOT NULL,
    endret_tid     TIMESTAMP(3) NOT NULL,
    saksbehandler  VARCHAR      NOT NULL,
    beslutter      VARCHAR,
    begrunnelse    VARCHAR,
    årsak          JSON,
    status         VARCHAR      NOT NULL

);
CREATE INDEX ON totrinnsstatus (behandling_id);