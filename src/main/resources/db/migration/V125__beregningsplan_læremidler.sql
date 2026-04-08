-- INNVILGELSE_LÆREMIDLER: Førstegangsbehandling
UPDATE vedtak v
SET data = v.data || jsonb_build_object(
    'beregningsplan', jsonb_build_object(
        'omfang', 'ALLE_PERIODER',
        'årsak', 'FØRSTEGANGS'
    )
)
FROM behandling b
WHERE v.behandling_id = b.id
  AND v.data->>'type' = 'INNVILGELSE_LÆREMIDLER'
  AND v.data->'beregningsplan' IS NULL
  AND b.forrige_iverksatte_behandling_id IS NULL;

-- INNVILGELSE_LÆREMIDLER: Satsendring (må komme før generell revurdering)
UPDATE vedtak v
SET data = v.data || jsonb_build_object(
    'beregningsplan', jsonb_build_object(
        'omfang', 'FRA_DATO',
        'årsak', 'SATSJUSTERING',
        'fraDato', to_char(v.tidligste_endring, 'YYYY-MM-DD')
    )
)
FROM behandling b
WHERE v.behandling_id = b.id
  AND v.data->>'type' = 'INNVILGELSE_LÆREMIDLER'
  AND v.data->'beregningsplan' IS NULL
  AND b.arsak = 'SATSENDRING';

-- INNVILGELSE_LÆREMIDLER: Revurdering med endring
UPDATE vedtak v
SET data = v.data || jsonb_build_object(
    'beregningsplan', jsonb_build_object(
        'omfang', 'FRA_DATO',
        'årsak', 'REVURDERING_MED_ENDRING',
        'fraDato', to_char(v.tidligste_endring, 'YYYY-MM-DD')
    )
)
FROM behandling b
WHERE v.behandling_id = b.id
  AND v.data->>'type' = 'INNVILGELSE_LÆREMIDLER'
  AND v.data->'beregningsplan' IS NULL
  AND b.forrige_iverksatte_behandling_id IS NOT NULL;

-- OPPHØR_LÆREMIDLER
UPDATE vedtak v
SET data = v.data || jsonb_build_object(
    'beregningsplan', jsonb_build_object(
        'omfang', 'FRA_DATO',
        'årsak', 'OPPHØR',
        'fraDato', to_char(v.opphorsdato, 'YYYY-MM-DD')
    )
)
WHERE v.data->>'type' = 'OPPHØR_LÆREMIDLER'
  AND v.data->'beregningsplan' IS NULL;
