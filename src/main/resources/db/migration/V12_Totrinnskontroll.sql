CREATE table totrinnsstatus
(
    id                                 UUID PRIMARY KEY,
    behandlings_id                      UUID NOT NULL REFERENCES behandling (id),
    opprettet_av                        VARCHAR      NOT NULL,
    opprettet_tid                       TIMESTAMP(3) NOT NULL,
    endret_av                           VARCHAR      NOT NULL,
    endret_tid                          TIMESTAMP(3) NOT NULL,
    saksbehandler                       VARCHAR,
    beslutter                           VARCHAR,
    begrunnelse                         VARCHAR,
    årsakerUnderkjent                   VARCHAR,
    status                              VARCHAR

);
CREATE INDEX ON totrinnsstatus(behandlings_Id);

CREATE INDEX ON totrinnsstatus(ID);