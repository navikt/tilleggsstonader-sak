CREATE table Totrinnsstatus
(
    id                                 UUID PRIMARY KEY,
    behandling_id                      UUID REFERENCES behandling (id),
    opprettet_av                       VARCHAR      NOT NULL,
    opprettet_tid                      TIMESTAMP(3) NOT NULL,
    endret_av                          VARCHAR      NOT NULL,
    endret_tid                         TIMESTAMP(3) NOT NULL,

    status                              VARCHAR,

)
CREATE INDEX ON totrinnsstatus(behandlingsId)
