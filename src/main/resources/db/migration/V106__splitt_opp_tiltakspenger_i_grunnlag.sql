-- Erstatter ytelse.kildeResultat i vilkårperiode_grunnlag der type="TILTAKSPENGER" med to nye elementer:
--   - TILTAKSPENGER_ARENA
--   - TILTAKSPENGER_TPSAK
-- ettersom TILTAKSPENGER har blitt splittet opp og ikke lenger er en gyldig ytelsestype

UPDATE vilkarperioder_grunnlag vg
SET grunnlag = jsonb_set(
        vg.grunnlag::jsonb,
        '{ytelse,kildeResultat}',
        (
            WITH old AS (
                SELECT jsonb_array_elements((vg.grunnlag::jsonb)->'ytelse'->'kildeResultat') AS elem
            ),
                 utenTiltakspenger AS (
                     -- Behold alle elementer bortsett fra "TILTAKSPENGER"
                     SELECT elem FROM old WHERE elem->>'type' <> 'TILTAKSPENGER'
                 ),
                 nyeElementer AS (
                     -- De to nye elementene vi skal legge til
                     SELECT jsonb_build_object('type','TILTAKSPENGER_ARENA','resultat','OK') AS elem
                     UNION ALL
                     SELECT jsonb_build_object('type','TILTAKSPENGER_TPSAK','resultat','OK')
                 )
            -- Bygg en ny array med både filtrerte og nye elementer
            SELECT jsonb_agg(elem)
            FROM (
                     SELECT * FROM utenTiltakspenger
                     UNION ALL
                     SELECT * FROM nyeElementer
                 ) all_elems
        )
               )
-- Oppdater kun rader som faktisk inneholder "TILTAKSPENGER" i kildeResultat
WHERE (vg.grunnlag::jsonb)->'ytelse'->'kildeResultat' @> '[{"type":"TILTAKSPENGER"}]'::jsonb;