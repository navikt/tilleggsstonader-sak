UPDATE vilkar
SET fakta = jsonb_set(
        fakta::jsonb,
        '{faktaDelperioder}',
        (
            SELECT jsonb_agg(
                           periode ||
                           jsonb_build_object(
                                   'skalBehandlesManuelt', false,
                                   'begrunnelseManuellBehandling', null
                           )
                   )
            FROM jsonb_array_elements(
                         fakta::jsonb -> 'faktaDelperioder'
                 ) AS periode
        )
            )::json
WHERE fakta IS NOT NULL
  AND fakta::jsonb ? 'faktaDelperioder';