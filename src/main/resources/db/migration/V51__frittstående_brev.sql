CREATE TABLE frittstaende_brev
(
    id                  UUID PRIMARY KEY,
    fagsak_id           UUID         NOT NULL REFERENCES fagsak (id),
    pdf                 BYTEA        NOT NULL,
    tittel              VARCHAR      NOT NULL,
    saksbehandler_ident VARCHAR      NOT NULL,
    opprettet_tid       TIMESTAMP(3) NOT NULL
);

CREATE TABLE brevmottaker_frittstaende_brev
(
    id                 UUID PRIMARY KEY,
    fagsak_id          UUID         NOT NULL REFERENCES fagsak (id),
    brev_id            UUID REFERENCES frittstaende_brev (id),
    journalpost_id     VARCHAR,
    bestilling_id      VARCHAR,

    ident              VARCHAR      NOT NULL,
    mottaker_rolle     VARCHAR      NOT NULL,
    mottaker_type      VARCHAR      NOT NULL,
    organisasjons_navn VARCHAR,
    mottaker_navn      VARCHAR,

    opprettet_av       VARCHAR      NOT NULL,
    opprettet_tid      TIMESTAMP(3) NOT NULL,
    endret_av          VARCHAR      NOT NULL,
    endret_tid         TIMESTAMP(3) NOT NULL
);

/**
  CONSTRAINT unik_mottaker_per_saksbehandler UNIQUE (fagsak_id, brev_id, ident, opprettet_av)
  Kan ikke brukes fordi brev_id = null ignoreres,
  og det skal ikke være mulig med duplikat av brevmottakere når brevet ennå ikke er opprettet
 */

CREATE UNIQUE INDEX unik_brevmottaker_frittstaende_not_null_idx
    ON brevmottaker_frittstaende_brev (fagsak_id, brev_id, ident, opprettet_av)
    WHERE brev_id IS NOT NULL;

CREATE UNIQUE INDEX unik_brevmottaker_frittstaende_null_idx
    ON brevmottaker_frittstaende_brev (fagsak_id, ident, opprettet_av)
    WHERE brev_id IS NULL;

