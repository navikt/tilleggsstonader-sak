CREATE TABLE vilkarperioder_grunnlag
(
    behandling_id UUID PRIMARY KEY REFERENCES behandling (id),
    grunnlag      JSON         NOT NULL,
    opprettet_av  VARCHAR      NOT NULL,
    opprettet_tid TIMESTAMP(3) NOT NULL,
    endret_av     VARCHAR      NOT NULL,
    endret_tid    TIMESTAMP(3) NOT NULL
)