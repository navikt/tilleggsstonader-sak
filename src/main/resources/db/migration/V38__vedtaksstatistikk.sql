CREATE TABLE vedtaksstatistikk
(
    id                     UUID PRIMARY KEY,
    fagsak_id              UUID    NOT NULL,
    behandling_id          UUID    NOT NULL,
    ekstern_fagsak_id      VARCHAR,
    ekstern_behandling_id  VARCHAR,
    relatert_behandling_id VARCHAR,
    adressebeskyttelse     VARCHAR,
    tidspunkt_vedtak       TIMESTAMP,
    malgrupper             JSON,
    aktiviteter            JSON,
    vilkarsvurderinger     JSON,
    person                 VARCHAR NOT NULL,
    barn                   JSON,
    behandling_type        VARCHAR,
    behandling_arsak       VARCHAR,
    vedtak_resultat        VARCHAR,
    vedtaksperioder        JSON    NOT NULL,
    utbetalinger           JSON    NOT NULL,
    stonadstype            VARCHAR,
    krav_mottatt           DATE
);
