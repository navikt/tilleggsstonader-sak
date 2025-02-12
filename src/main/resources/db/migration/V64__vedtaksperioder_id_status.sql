UPDATE vedtak
SET data = jsonb_set(
        data,
        '{vedtaksperioder}',
        (SELECT jsonb_agg(
                        CASE
                            WHEN vedtaksperiode -> 'status' IS NULL AND vedtaksperiode -> 'id' IS NULL THEN
                                jsonb_set(
                                        jsonb_set(vedtaksperiode, '{status}', '"NY"'),
                                        '{id}',
                                        to_jsonb(gen_random_uuid())
                                )
                            ELSE
                                vedtaksperiode -- Ikke tafs på vedtaksperioder som allerede har status og id
                            END
                )
         FROM jsonb_array_elements(data -> 'vedtaksperioder') vedtaksperiode)
           )
WHERE data -> 'vedtaksperioder' IS NOT NULL;