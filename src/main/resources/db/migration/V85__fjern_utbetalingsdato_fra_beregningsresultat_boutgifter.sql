UPDATE vedtak
SET data = jsonb_set(
        data,
        '{beregningsresultat,perioder}',
        (SELECT jsonb_agg(
                        vedtaksdata - 'grunnlag' ||
                        jsonb_build_object('grunnlag', (vedtaksdata -> 'grunnlag') - 'utbetalingsdato')
                )
         FROM jsonb_array_elements(data -> 'beregningsresultat' -> 'perioder') AS vedtaksdata)
           )
WHERE data ->> 'type' IN ('INNVILGELSE_BOUTGIFTER', 'OPPHØR_BOUTGIFTER');