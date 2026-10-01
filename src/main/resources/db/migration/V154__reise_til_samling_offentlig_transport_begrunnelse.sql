UPDATE vedtak v
SET data = jsonb_set(
        v.data,
        '{beregningsresultat,offentligTransport}',
        (
            SELECT COALESCE(
                           jsonb_agg(
                                   CASE
                                       WHEN reise ->> 'begrunnelse' IS NOT NULL
                                           THEN reise
                                       ELSE reise || jsonb_build_object('begrunnelse', '')
                                       END
                                   ORDER BY ordinality
                           ),
                           '[]'::jsonb
                   )
            FROM jsonb_array_elements(v.data -> 'beregningsresultat' -> 'offentligTransport')
                     WITH ORDINALITY AS reiser(reise, ordinality)
        ),
        false
    )
WHERE v.data ->> 'type' IN ('INNVILGELSE_REISE_TIL_SAMLING', 'OPPHØR_REISE_TIL_SAMLING')
  AND v.data -> 'beregningsresultat' -> 'offentligTransport' IS NOT NULL
  AND jsonb_typeof(v.data -> 'beregningsresultat' -> 'offentligTransport') = 'array'
  AND EXISTS(
        SELECT 1
        FROM jsonb_array_elements(v.data -> 'beregningsresultat' -> 'offentligTransport') AS reise
        WHERE reise ->> 'begrunnelse' IS NULL
    );
