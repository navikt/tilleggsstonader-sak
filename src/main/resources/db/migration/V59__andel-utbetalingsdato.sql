/**
  Setter utbetalingsdato til første ukedag i måneden for eksisterende andeler
  For fremtidige andeler kommer utbetalingsdatoet eks settes ulikt for læremidler
 */
ALTER TABLE andel_tilkjent_ytelse
    ADD COLUMN utbetalingsdato DATE;
UPDATE andel_tilkjent_ytelse
SET utbetalingsdato=
        CASE
            WHEN EXTRACT(DOW FROM date_trunc('MONTH', fom)) = 0
                THEN date_trunc('MONTH', fom) + INTERVAL '1 day' -- Hvis søndag
            WHEN EXTRACT(DOW FROM date_trunc('MONTH', fom)) = 6
                THEN date_trunc('MONTH', fom) + INTERVAL '2 days' -- Hvis lørdag
            ELSE date_trunc('MONTH', fom)
            END
;

ALTER TABLE andel_tilkjent_ytelse
    ALTER COLUMN utbetalingsdato SET NOT NULL;

-- TODO må oppdatere task med riktig dato som den skal kjøres?
--UPDATE task set WHERE type = 'IverksettMåned' AND status = '';