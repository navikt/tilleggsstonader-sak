ALTER TABLE andel_tilkjent_ytelse
    ADD COLUMN utbetalingsmaned DATE;

UPDATE andel_tilkjent_ytelse
SET utbetalingsmaned = date_trunc('month', fom)::date;

ALTER TABLE andel_tilkjent_ytelse
    ALTER COLUMN utbetalingsmaned SET NOT NULL;