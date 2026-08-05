-- Migrerer perioder i grunnlag->'ytelse'->'perioder' fra flat dataklasse-format
-- til sealed interface-format. Fjerner fremmede null-felt fra hvert periode-element
-- og beholder kun feltene som er relevante for den aktuelle typen.
--
-- Gammelt format (alle typer hadde samme flate struktur):
--   {"type":"AAP","fom":"...","tom":null,"subtype":null,"gjenståendeDagerFraTelleverk":null,"erNyttRegelverk2026":null}
--
-- Nytt format (kun relevante felt per type):
--   AAP:              {"type":"AAP","fom":"...","tom":null,"subtype":null}
--   DAGPENGER:        {"type":"DAGPENGER","fom":"...","tom":null,"gjenståendeDagerFraTelleverk":null}
--   ENSLIG_FORSØRGER: {"type":"ENSLIG_FORSØRGER","fom":"...","tom":null,"subtype":null,"erNyttRegelverk2026":null}
--   Andre:            {"type":"...","fom":"...","tom":null}

UPDATE vilkarperioder_grunnlag vg
SET grunnlag = jsonb_set(
    vg.grunnlag::jsonb,
    '{ytelse,perioder}',
    (
        SELECT jsonb_agg(
            CASE periode->>'type'
                WHEN 'AAP' THEN
                    jsonb_build_object(
                        'type', periode->>'type',
                        'fom', periode->'fom',
                        'tom', periode->'tom',
                        'subtype', periode->'subtype'
                    )
                WHEN 'DAGPENGER' THEN
                    jsonb_build_object(
                        'type', periode->>'type',
                        'fom', periode->'fom',
                        'tom', periode->'tom',
                        'gjenståendeDagerFraTelleverk', periode->'gjenståendeDagerFraTelleverk'
                    )
                WHEN 'ENSLIG_FORSØRGER' THEN
                    jsonb_build_object(
                        'type', periode->>'type',
                        'fom', periode->'fom',
                        'tom', periode->'tom',
                        'subtype', periode->'subtype',
                        'erNyttRegelverk2026', periode->'erNyttRegelverk2026'
                    )
                ELSE
                    -- OMSTILLINGSSTØNAD, TILTAKSPENGER_TPSAK, TILTAKSPENGER_ARENA
                    jsonb_build_object(
                        'type', periode->>'type',
                        'fom', periode->'fom',
                        'tom', periode->'tom'
                    )
            END
        )
        FROM jsonb_array_elements((vg.grunnlag::jsonb)->'ytelse'->'perioder') AS periode
    )
)
WHERE (vg.grunnlag::jsonb)->'ytelse'->'perioder' IS NOT NULL
  AND jsonb_array_length((vg.grunnlag::jsonb)->'ytelse'->'perioder') > 0;
