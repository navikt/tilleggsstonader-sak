CREATE table totrinnsstatus
(
    id             UUID PRIMARY KEY,
    behandlings_id UUID NOT NULL REFERENCES behandling (id),
    opprettet_av   VARCHAR NOT NULL,
    opprettet_tid  TIMESTAMP(3) NOT NULL,
    endret_av      VARCHAR NOT NULL,
    sporbar        TIMESTAMP(3) NOT NULL,
    saksbehandler  JSON,
    beslutter      JSON,
    begrunnelse    JSON,
    årsak          JSON,
    status         JSON

);
CREATE INDEX ON totrinnsstatus (behandlings_Id);