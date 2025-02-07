UPDATE vedtak
SET data = jsonb_set(
        data,
        '{vedtaksperioder}',
        (SELECT jsonb_agg(
                        jsonb_set(
                                jsonb_set(vedtaksperiode, '{status}', '"NY"'),
                                '{id}',
                                to_jsonb(gen_random_uuid())
                        )
                )
         FROM jsonb_array_elements(data -> 'vedtaksperioder') vedtaksperiode)
           );