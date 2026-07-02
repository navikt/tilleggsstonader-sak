-- Kjørelistebehandlinger skal alltid bruke beregningsomfang KUN_NYE_KJORELISTE_UKER
-- uten fraDato/tidligsteEndring.
UPDATE vedtak v
SET data = jsonb_set(
        v.data,
        '{beregningsplan}',
        jsonb_build_object('omfang', 'KUN_NYE_KJORELISTE_UKER')
    ),
    tidligste_endring = NULL
FROM behandling b
WHERE v.behandling_id = b.id
  AND b.type = 'KJØRELISTE';
