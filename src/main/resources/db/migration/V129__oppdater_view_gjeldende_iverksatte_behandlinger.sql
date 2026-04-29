DROP VIEW IF EXISTS gjeldende_iverksatte_behandlinger;

CREATE OR REPLACE VIEW gjeldende_iverksatte_behandlinger AS
SELECT *
FROM (
         SELECT f.stonadstype,
                ROW_NUMBER() OVER (PARTITION BY b.fagsak_id ORDER BY b.vedtakstidspunkt DESC) rn,
                f.fagsak_person_id,
                b.*
         FROM behandling b
                  JOIN fagsak f ON b.fagsak_id = f.id
         WHERE b.resultat IN ('OPPHØRT', 'INNVILGET')
           AND b.status = 'FERDIGSTILT'
     ) q
WHERE rn = 1;

