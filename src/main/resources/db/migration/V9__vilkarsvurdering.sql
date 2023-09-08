CREATE TABLE vilkarsvurdering
(
    id                                 UUID PRIMARY KEY,
    behandling_id                      UUID REFERENCES behandling (id),
    opprettet_av                       VARCHAR      NOT NULL,
    opprettet_tid                      TIMESTAMP(3) NOT NULL,
    endret_av                          VARCHAR      NOT NULL,
    endret_tid                         TIMESTAMP(3) NOT NULL,

    resultat                           VARCHAR      NOT NULL,
    type                               VARCHAR      NOT NULL,
    begrunnelse                        VARCHAR,
    unntak                             VARCHAR,
    delvilkar                          JSON,
    barn_id                            VARCHAR,
    opphavsvilkaar_behandling_id       UUID,
    opphavsvilkaar_vurderingstidspunkt TIMESTAMP(3)
);

CREATE INDEX ON vilkarsvurdering (behandling_id);

