ALTER TABLE behandling
    ADD COLUMN behandling_metode VARCHAR;

UPDATE behandling
SET behandling_metode = 'BATCH'
WHERE behandling.arsak = 'SATSENDRING';

UPDATE behandling
SET behandling_metode = 'MANUELL'
WHERE behandling_metode IS NULL;

ALTER TABLE behandling
    ALTER COLUMN behandling_metode SET NOT NULL;

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
