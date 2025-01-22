CREATE TABLE vedtaksstatistikk_v2
(
    id                     UUID PRIMARY KEY,
    fagsak_id              UUID         NOT NULL,
    behandling_id          UUID         NOT NULL,
    ekstern_fagsak_id      VARCHAR      NOT NULL,
    ekstern_behandling_id  VARCHAR      NOT NULL,
    relatert_behandling_id VARCHAR,
    adressebeskyttelse     VARCHAR,
    tidspunkt_vedtak       TIMESTAMP(3) NOT NULL,
    person                 VARCHAR      NOT NULL,
    behandling_type        VARCHAR      NOT NULL,
    behandling_arsak       VARCHAR      NOT NULL,
    vedtak_resultat        VARCHAR      NOT NULL,
    vedtaksperioder        JSON         NOT NULL,
    utbetalinger           JSON         NOT NULL,
    stonadstype            VARCHAR      NOT NULL,
    krav_mottatt           DATE,
    arsaker_avslag         JSON,
    arsaker_opphor         JSON,
    opprettet_tid          TIMESTAMP(3) NOT NULL,
    endret_tid             TIMESTAMP(3) NOT NULL
);