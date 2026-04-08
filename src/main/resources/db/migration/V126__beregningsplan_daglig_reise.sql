-- INNVILGELSE: Førstegangsbehandling eller revurdering av avslag (ingen tidligste_endring)
UPDATE vedtak v
SET data = v.data || jsonb_build_object(
    'beregningsplan', jsonb_build_object(
        'omfang', 'ALLE_PERIODER'
    )
)
WHERE v.data->>'type' = 'INNVILGELSE_DAGLIG_REISE'
  AND v.data->'beregningsplan' IS NULL
  AND v.tidligste_endring IS NULL;

-- INNVILGELSE: Revurdering med endring
UPDATE vedtak v
SET data = v.data || jsonb_build_object(
    'beregningsplan', jsonb_build_object(
        'omfang', 'FRA_DATO',
        'fraDato', to_char(v.tidligste_endring, 'YYYY-MM-DD')
    )
)
WHERE v.data->>'type' = 'INNVILGELSE_DAGLIG_REISE'
  AND v.data->'beregningsplan' IS NULL
  AND v.tidligste_endring IS NOT NULL;

-- OPPHØR
UPDATE vedtak v
SET data = v.data || jsonb_build_object(
    'beregningsplan', jsonb_build_object(
        'omfang', 'FRA_DATO',
        'fraDato', to_char(v.opphorsdato, 'YYYY-MM-DD')
    )
)
WHERE v.data->>'type' = 'OPPHØR_DAGLIG_REISE'
  AND v.data->'beregningsplan' IS NULL;
