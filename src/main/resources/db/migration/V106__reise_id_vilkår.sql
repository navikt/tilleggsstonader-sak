UPDATE vilkar
SET fakta = jsonb_set(
        fakta::jsonb,
        '{reiser}',
        (
            SELECT jsonb_agg(
                           CASE
                               WHEN reise ? 'reiseId' THEN reise
                               ELSE jsonb_set(
                                       reise::jsonb,
                                       '{reiseId}',
                                       to_jsonb(gen_random_uuid())
                                    )
                               END
                   )
            FROM jsonb_array_elements((fakta -> 'reiser')::jsonb) AS reise
        ),
        true
            )
WHERE fakta -> 'reiser' IS NOT NULL;
