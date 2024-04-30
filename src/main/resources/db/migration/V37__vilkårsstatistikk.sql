CREATE TABLE vilkarsstatistikk
(
    fagsakId             UUID    NOT NULL,
    behandlingId         UUID    NOT NULL,
    relatertBehandlingId UUID,
    adressebeskyttelse   VARCHAR,
    tidspunktVedtak      TIMESTAMP,
    malgruppe            VARCHAR,
    aktivitet            VARCHAR,
    vilkarsvurderinger   JSON,
    person               VARCHAR NOT NULL,
    barn                 JSON,
    behandlingType       VARCHAR,
    behandlingArsak      VARCHAR,
    vedtak               VARCHAR,
    vedtaksperioder      JSON    NOT NULL,
    utbetalinger         JSON    NOT NULL,
    stonadstype          VARCHAR,
    kravMottatt          DATE,
    arsakRevurdering     VARCHAR,
    avslagArsak          VARCHAR
);
