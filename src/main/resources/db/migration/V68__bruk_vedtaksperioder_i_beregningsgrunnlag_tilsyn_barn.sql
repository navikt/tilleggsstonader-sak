UPDATE vedtak
SET data = jsonb_set(
        data,
        '{beregningsresultat}',
        replace(
                replace(data ->> 'beregningsresultat', '"stønadsperioderGrunnlag"', '"vedtaksperiodeGrunnlag"'),
                '"stønadsperiode"', '"vedtaksperiode"'
        )::jsonb
           )
WHERE data ->> 'type' IN ('INNVILGELSE_TILSYN_BARN', 'OPPHØR_TILSYN_BARN');