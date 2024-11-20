ALTER TABLE vedtak_tilsyn_barn
    rename to vedtak;
-- Lager en backup for vilkar_periode som senere kan slettes
CREATE TABLE vedtak_20241120 AS (SELECT *
                                 FROM vedtak);
ALTER TABLE vedtak
    ADD COLUMN data JSONB;

-- INNVILGELSE
UPDATE vedtak
SET data=jsonb_build_object(
        'type', type || '_TILSYN_BARN',
        'beregningsresultat', beregningsresultat
         )
WHERE type = 'INNVILGELSE'
;

-- OPPHØR
UPDATE vedtak
SET data=jsonb_build_object(
        'type', type || '_TILSYN_BARN',
        'beregningsresultat', beregningsresultat,
        'begrunnelse', opphor_begrunnelse,
        'årsaker', arsaker_opphor -> 'årsaker'
         )
WHERE type = 'OPPHØR'
;

-- AVSLAG
UPDATE vedtak
SET data=jsonb_build_object(
        'type', type || '_TILSYN_BARN',
        'begrunnelse', avslag_begrunnelse,
        'årsaker', arsaker_avslag -> 'årsaker'
         )
WHERE type = 'AVSLAG'
;

ALTER TABLE vedtak
    ALTER COLUMN data SET NOT NULL;

-- Sletter tidligere kolonner som nå ligger i data
ALTER TABLE vedtak
    DROP COLUMN vedtak,
    DROP COLUMN beregningsresultat,
    DROP COLUMN arsaker_avslag,
    DROP COLUMN avslag_begrunnelse,
    DROP COLUMN arsaker_opphor,
    DROP COLUMN opphor_begrunnelse;
