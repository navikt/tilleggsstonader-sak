CREATE TABLE behandling
(
    id                    UUID PRIMARY KEY,
    fagsak_id             UUID         NOT NULL REFERENCES fagsak (id),
    versjon               INTEGER,
    opprettet_av          VARCHAR      NOT NULL,
    opprettet_tid         TIMESTAMP(3) NOT NULL,
    endret_av             VARCHAR      NOT NULL,
    endret_tid            TIMESTAMP(3) NOT NULL,
    type                  VARCHAR      NOT NULL,
    status                VARCHAR      NOT NULL,
    steg                  VARCHAR      NOT NULL,
    kategori              VARCHAR      NOT NULL,
    resultat              VARCHAR      NOT NULL,
    forrige_behandling_id UUID REFERENCES behandling (id),
    arsak                 VARCHAR      NOT NULL,
    krav_mottatt          TIMESTAMP(3),
    henlagt_arsak         VARCHAR,
    vedtakstidspunkt      TIMESTAMP(3)
);

ALTER TABLE behandling
    ADD CONSTRAINT behandling_resultat_vedtakstidspunkt_check
        CHECK ((resultat = 'IKKE_SATT' AND vedtakstidspunkt IS null)
            OR
               (resultat <> 'IKKE_SATT' AND vedtakstidspunkt IS NOT null));

CREATE UNIQUE INDEX idx_behandlinger_i_arbeid
    ON behandling (fagsak_id) WHERE (status <> 'FERDIGSTILT' AND status <> 'SATT_PÅ_VENT');


CREATE TABLE behandling_ekstern
(
    id            BIGSERIAL PRIMARY KEY,
    behandling_id UUID NOT NULL REFERENCES behandling (id)
);

CREATE INDEX ON behandling_ekstern (behandling_id);

/**
  View som returnerer alle gjeldende iverksatte behandlinger
  Husk å filtrere på stønadstype ved behov
 */
CREATE OR REPLACE VIEW gjeldende_iverksatte_behandlinger AS
SELECT *
FROM (SELECT f.stonadstype,
             ROW_NUMBER() OVER (PARTITION BY b.fagsak_id ORDER BY b.vedtakstidspunkt DESC) rn,
             f.fagsak_person_id,
             b.*
      FROM behandling b
               JOIN fagsak f ON b.fagsak_id = f.id
      WHERE b.resultat IN ('OPPHØRT', 'INNVILGET')
        AND b.status = 'FERDIGSTILT') q
WHERE rn = 1;