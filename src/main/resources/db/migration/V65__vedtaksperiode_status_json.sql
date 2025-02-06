UPDATE vedtak
SET data = jsonb_set(
        data,
        '{vedtaksperioder}',
        (SELECT jsonb_agg(
                        jsonb_set(vedtaksperiode, '{status}', '"UENDRET"')
                )
         FROM jsonb_array_elements(data -> 'vedtaksperioder') vedtaksperiode)
           )
;