UPDATE vedtak
SET data = replace(
        replace(data::text, '"stønadsperioderGrunnlag"', '"vedtaksperiodeGrunnlag"'),
        '"stønadsperiode"', '"vedtaksperiode"'
           )::jsonb
WHERE data ->> 'type' IN ('INNVILGELSE_TILSYN_BARN', 'OPPHØR_TILSYN_BARN');