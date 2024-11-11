ALTER TABLE vilkar_periode
    ADD COLUMN fakta_og_vurdering JSONB;

-- noinspection SqlWithoutWhere
UPDATE vilkar_periode
SET fakta_og_vurdering = jsonb_build_object(
        'type', type || '_TILSYN_BARN',
        'vurderinger', CASE
                           WHEN delvilkar -> 'lønnet' IS NOT NULL AND
                                delvilkar -> 'lønnet' ->> 'resultat' <> 'IKKE_AKTUELT'
                               THEN jsonb_build_object('lønnet', delvilkar -> 'lønnet')
                           ELSE '{}'::jsonb
                           END
                           ||
                       CASE
                           WHEN delvilkar -> 'dekketAvAnnetRegelverk' IS NOT NULL AND
                                delvilkar -> 'dekketAvAnnetRegelverk' ->> 'resultat' <> 'IKKE_AKTUELT'
                               THEN jsonb_build_object('dekketAvAnnetRegelverk',
                                                       delvilkar -> 'dekketAvAnnetRegelverk')
                           ELSE '{}'::jsonb
                           END
            ||
                       CASE
                           WHEN delvilkar -> 'medlemskap' IS NOT NULL AND
                                delvilkar -> 'medlemskap' ->> 'resultat' <> 'IKKE_AKTUELT'
                               THEN jsonb_build_object('medlemskap', delvilkar -> 'medlemskap')
                           ELSE '{}'::jsonb
                           END
    ,
        'fakta',
        CASE
            WHEN aktivitetsdager IS NOT NULL THEN jsonb_build_object('aktivitetsdager', aktivitetsdager)
            ELSE '{}'::jsonb
            END
                         );

ALTER TABLE vilkar_periode
    DROP COLUMN delvilkar;
ALTER TABLE vilkar_periode
    DROP COLUMN aktivitetsdager;
ALTER TABLE vilkar_periode
    ALTER COLUMN fakta_og_vurdering SET NOT NULL;
