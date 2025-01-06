/**
  Setter utbetalingsdato til første ukedag i måneden for eksisterende andeler
  For fremtidige andeler kommer utbetalingsdatoet eks settes ulikt for læremidler
 */
ALTER TABLE andel_tilkjent_ytelse
    ADD COLUMN utbetalingsdato DATE;

-- noinspection SqlWithoutWhere
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

/**
  Endrer neste kjøring fra månedskjøring til daglig kjøring sånn at den kjører hver dag i stedet
 */
UPDATE TASK
SET type        = 'DagligIverksett',
    payload     = current_date,
    trigger_tid = current_timestamp + interval '1 hour'
WHERE type = 'IverksettMåned'
  AND status = 'UBEHANDLET';