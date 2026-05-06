ALTER TABLE avklart_kjort_uke
    ADD COLUMN avklart_kjort_uke_status VARCHAR NOT NULL DEFAULT 'NY';

ALTER TABLE avklart_kjort_uke
    ALTER COLUMN avklart_kjort_uke_status DROP DEFAULT;

UPDATE vedtak
SET data = jsonb_set(
    data,
    '{beregningsresultat,privatBil,reiser}',
    (SELECT jsonb_agg(
        jsonb_set(reise, '{perioder}',
            (SELECT jsonb_agg(periode || '{"fraTidligereVedtak": false}'::jsonb)
             FROM jsonb_array_elements(reise -> 'perioder') AS periode)
        )
    )
    FROM jsonb_array_elements(data -> 'beregningsresultat' -> 'privatBil' -> 'reiser') AS reise)
)
WHERE data -> 'beregningsresultat' -> 'privatBil' IS NOT NULL
  AND jsonb_typeof(data -> 'beregningsresultat' -> 'privatBil') != 'null';
