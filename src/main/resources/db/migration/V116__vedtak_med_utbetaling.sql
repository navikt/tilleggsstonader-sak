ALTER TABLE vedtak
    ADD COLUMN med_utbetaling BOOLEAN;

UPDATE vedtak
SET med_utbetaling =
        CASE
            WHEN type IN ('INNVILGELSE', 'OPPHØR') THEN TRUE
            ELSE FALSE
            END;

ALTER TABLE vedtak
    ALTER COLUMN med_utbetaling SET NOT NULL ;