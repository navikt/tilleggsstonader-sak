-- TODO
ALTER TABLE vilkar_periode
    ADD COLUMN fakta_og_vurdering JSONB;

UPDATE vilkar_periode
SET fakta_og_vurdering =
        jsonb_build_object(
                'vurderinger', delvilkar,
                'fakta',
                CASE
                    WHEN aktivitetsdager IS NOT NULL THEN jsonb_build_object('aktivitetsdager', aktivitetsdager)
                    ELSE '{}'::jsonb
                    END
        );

ALTER TABLE vilkar_periode
    DROP COLUMN delvilkar;
ALTER TABLE vilkar_periode
    ALTER COLUMN fakta_og_vurdering SET NOT NULL;