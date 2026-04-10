-- INNVILGELSE: Førstegangsbehandling eller revurdering av avslag (ingen tidligste_endring)
UPDATE vedtak
SET data = data || jsonb_build_object(
    'beregningsplan', jsonb_build_object(
        'omfang', 'ALLE_PERIODER'
    )
)
WHERE data->>'type' IN (
    'INNVILGELSE_TILSYN_BARN',
    'INNVILGELSE_BOUTGIFTER',
    'INNVILGELSE_LÆREMIDLER',
    'INNVILGELSE_DAGLIG_REISE'
)
  AND data->'beregningsplan' IS NULL
  AND tidligste_endring IS NULL;

-- INNVILGELSE: Revurdering med endring
UPDATE vedtak
SET data = data || jsonb_build_object(
    'beregningsplan', jsonb_build_object(
        'omfang', 'FRA_DATO',
        'fraDato', to_char(tidligste_endring, 'YYYY-MM-DD')
    )
)
WHERE data->>'type' IN (
    'INNVILGELSE_TILSYN_BARN',
    'INNVILGELSE_BOUTGIFTER',
    'INNVILGELSE_LÆREMIDLER',
    'INNVILGELSE_DAGLIG_REISE'
)
  AND data->'beregningsplan' IS NULL
  AND tidligste_endring IS NOT NULL;

-- OPPHØR
UPDATE vedtak
SET data = data || jsonb_build_object(
    'beregningsplan', jsonb_build_object(
        'omfang', 'FRA_DATO',
        'fraDato', to_char(opphorsdato, 'YYYY-MM-DD')
    )
)
WHERE data->>'type' IN (
    'OPPHØR_TILSYN_BARN',
    'OPPHØR_BOUTGIFTER',
    'OPPHØR_LÆREMIDLER',
    'OPPHØR_DAGLIG_REISE'
)
  AND data->'beregningsplan' IS NULL;
