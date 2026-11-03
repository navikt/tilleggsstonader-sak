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
WHERE data::jsonb->'rammevedtakPrivatBil' IS NOT NULL;