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
  AND fakta::jsonb ? 'faktaDelperioder'
  AND jsonb_array_length(
        NULLIF(fakta::jsonb -> 'faktaDelperioder', 'null'::jsonb)
      ) > 0;


UPDATE vedtak
SET data = (
    SELECT jsonb_set(
                   data::jsonb,
                   '{rammevedtakPrivatBil,reiser}',
                   (
                       SELECT jsonb_agg(
                                      jsonb_set(
                                              reise,
                                              '{grunnlag,delperioder}',
                                              (
                                                  SELECT jsonb_agg(
                                                                 delperiode || jsonb_build_object(
                                                                         'skalBehandlesManuelt', false,
                                                                         'begrunnelseManuellBehandling', NULL
                                                                               )
                                                         )
                                                  FROM jsonb_array_elements(
                                                               reise->'grunnlag'->'delperioder'
                                                       ) AS delperiode
                                              )
                                      )
                              )
                       FROM jsonb_array_elements(
                                    data::jsonb->'rammevedtakPrivatBil'->'reiser'
                            ) AS reise
                   )
           )::json
)
WHERE data::jsonb->'rammevedtakPrivatBil' IS NOT NULL
  AND jsonb_array_length(
        NULLIF(data::jsonb -> 'rammevedtakPrivatBil' -> 'reiser', 'null'::jsonb)
      ) > 0;