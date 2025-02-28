WITH transformertVedtaksperiodeGrunnlag AS (SELECT behandling_id,
                                                   jsonb_set(
                                                           data,
                                                           '{beregningsresultat,perioder}',
                                                           (SELECT jsonb_agg(
                                                                           jsonb_set(
                                                                                   periode,
                                                                                   '{grunnlag,vedtaksperiodeGrunnlag}',
                                                                                   periode #>
                                                                                   '{grunnlag,stønadsperioderGrunnlag}'
                                                                           ) #- '{grunnlag,stønadsperioderGrunnlag}'
                                                                   )
                                                            FROM jsonb_array_elements(data -> 'beregningsresultat' -> 'perioder') AS periode)
                                                   ) AS transformert_json
                                            FROM vedtak
                                            WHERE data ->> 'type' IN ('INNVILGELSE_TILSYN_BARN', 'OPPHØR_TILSYN_BARN'))
UPDATE vedtak
SET data = (SELECT jsonb_set(
                           transformert_json,
                           '{beregningsresultat,perioder}',
                           (SELECT jsonb_agg(
                                           jsonb_set(
                                                   periode,
                                                   '{grunnlag,vedtaksperiodeGrunnlag}',
                                                   (SELECT jsonb_agg(
                                                                   jsonb_set(
                                                                           elem,
                                                                           '{vedtaksperiode}',
                                                                           elem #> '{stønadsperiode}'
                                                                   ) #- '{stønadsperiode}'
                                                           )
                                                    FROM jsonb_array_elements(periode #> '{grunnlag,vedtaksperiodeGrunnlag}') AS elem)
                                           )
                                   )
                            FROM jsonb_array_elements(transformert_json -> 'beregningsresultat' -> 'perioder') AS periode)
                   )
            FROM transformertVedtaksperiodeGrunnlag
            WHERE vedtak.behandling_id = transformertVedtaksperiodeGrunnlag.behandling_id)
WHERE vedtak.data ->> 'type' IN ('INNVILGELSE_TILSYN_BARN', 'OPPHØR_TILSYN_BARN');