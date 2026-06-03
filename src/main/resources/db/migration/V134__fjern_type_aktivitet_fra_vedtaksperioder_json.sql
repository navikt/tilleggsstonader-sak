-- Fjerner typeAktivitet fra toppnivå-vedtaksperioder i vedtak.data
UPDATE vedtak v
SET data =
        jsonb_set(
                v.data,
                '{vedtaksperioder}',
                (SELECT COALESCE(
                                jsonb_agg(vp - 'typeAktivitet'),
                                '[]'::jsonb
                        )
                 FROM jsonb_array_elements(COALESCE(v.data -> 'vedtaksperioder', '[]'::jsonb)) AS vp),
                false
        )
WHERE v.data -> 'vedtaksperioder' IS NOT NULL
  AND EXISTS (SELECT 1
              FROM jsonb_array_elements(COALESCE(v.data -> 'vedtaksperioder', '[]'::jsonb)) AS vp
              WHERE vp ? 'typeAktivitet');

-- Fjerner typeAktivitet fra vedtaksperioder i OT-beregningsresultat:
-- beregningsresultat.offentligTransport.reiser[*].perioder[*].grunnlag.vedtaksperioder[*].typeAktivitet
UPDATE vedtak v
SET data =
        jsonb_set(
                v.data,
                '{beregningsresultat,offentligTransport,reiser}',
                (SELECT COALESCE(
                                jsonb_agg(
                                        jsonb_set(
                                                reise,
                                                '{perioder}',
                                                (SELECT COALESCE(
                                                                jsonb_agg(
                                                                        jsonb_set(
                                                                                periode,
                                                                                '{grunnlag,vedtaksperioder}',
                                                                                (SELECT COALESCE(
                                                                                                jsonb_agg(vp - 'typeAktivitet'),
                                                                                                '[]'::jsonb
                                                                                        )
                                                                                 FROM jsonb_array_elements(
                                                                                              COALESCE(periode -> 'grunnlag' -> 'vedtaksperioder', '[]'::jsonb)
                                                                                      ) AS vp),
                                                                                false
                                                                        )
                                                                ),
                                                                '[]'::jsonb
                                                        )
                                                 FROM jsonb_array_elements(COALESCE(reise -> 'perioder', '[]'::jsonb)) AS periode),
                                                false
                                        )
                                ),
                                '[]'::jsonb
                        )
                 FROM jsonb_array_elements(COALESCE(v.data -> 'beregningsresultat' -> 'offentligTransport' -> 'reiser',
                                                    '[]'::jsonb)) AS reise),
                false
        )
WHERE v.data -> 'beregningsresultat' -> 'offentligTransport' -> 'reiser' IS NOT NULL
  AND EXISTS (SELECT 1
              FROM jsonb_array_elements(COALESCE(v.data -> 'beregningsresultat' -> 'offentligTransport' -> 'reiser',
                                                 '[]'::jsonb)) AS reise,
                   jsonb_array_elements(COALESCE(reise -> 'perioder', '[]'::jsonb)) AS periode,
                   jsonb_array_elements(COALESCE(periode -> 'grunnlag' -> 'vedtaksperioder', '[]'::jsonb)) AS vp
              WHERE vp ? 'typeAktivitet');
