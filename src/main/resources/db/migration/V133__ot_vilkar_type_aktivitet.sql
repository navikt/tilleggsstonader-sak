-- Kopierer typeAktivitet fra beregningsresultatet i vedtaket til vilkårenes fakta-JSONB
-- for offentlig transport-vilkår som mangler typeAktivitet.
-- Kobling skjer via reiseId: vilkår.fakta->>'reiseId' = reise->>'reiseId' i vedtak.data
UPDATE vilkar v
SET fakta = v.fakta::jsonb || jsonb_build_object(
        'typeAktivitet',
        (
            SELECT reise -> 'perioder' -> 0 -> 'grunnlag' -> 'vedtaksperioder' -> 0 ->> 'typeAktivitet'
            FROM vedtak vd,
                 jsonb_array_elements(
                         vd.data -> 'beregningsresultat' -> 'offentligTransport' -> 'reiser'
                 ) AS reise
            WHERE vd.behandling_id = v.behandling_id
              AND reise ->> 'reiseId' = v.fakta ->> 'reiseId'
              AND reise -> 'perioder' -> 0 -> 'grunnlag' -> 'vedtaksperioder' -> 0 ->> 'typeAktivitet' IS NOT NULL
            LIMIT 1
        )
)
WHERE v.fakta ->> 'type' = 'DAGLIG_REISE_OFFENTLIG_TRANSPORT'
  AND v.fakta ->> 'typeAktivitet' IS NULL
  AND EXISTS (
    SELECT 1
    FROM vedtak vd2,
         jsonb_array_elements(
                 vd2.data -> 'beregningsresultat' -> 'offentligTransport' -> 'reiser'
         ) AS reise2
    WHERE vd2.behandling_id = v.behandling_id
      AND reise2 ->> 'reiseId' = v.fakta ->> 'reiseId'
      AND reise2 -> 'perioder' -> 0 -> 'grunnlag' -> 'vedtaksperioder' -> 0 ->> 'typeAktivitet' IS NOT NULL
);

-- Kopierer typeAktivitet fra vedtaksperiodenes grunnlag opp til reise-nivå i beregningsresultatet.
-- typeAktivitet er lik for alle vedtaksperioder innenfor en reise (kun TSR).
UPDATE vedtak vd
SET data = jsonb_set(
    vd.data,
    '{beregningsresultat,offentligTransport,reiser}',
    (
        SELECT jsonb_agg(
            CASE
                WHEN reise ->> 'typeAktivitet' IS NULL
                    AND reise -> 'perioder' -> 0 -> 'grunnlag' -> 'vedtaksperioder' -> 0 ->> 'typeAktivitet' IS NOT NULL
                THEN reise || jsonb_build_object(
                    'typeAktivitet',
                    reise -> 'perioder' -> 0 -> 'grunnlag' -> 'vedtaksperioder' -> 0 ->> 'typeAktivitet'
                )
                ELSE reise
            END
        )
        FROM jsonb_array_elements(
            vd.data -> 'beregningsresultat' -> 'offentligTransport' -> 'reiser'
        ) AS reise
    )
)
WHERE vd.data -> 'beregningsresultat' -> 'offentligTransport' IS NOT NULL
  AND EXISTS (
    SELECT 1
    FROM behandling b
    JOIN fagsak f ON f.id = b.fagsak_id
    WHERE b.id = vd.behandling_id
      AND f.stonadstype = 'DAGLIG_REISE_TSR'
  )
  AND EXISTS (
    SELECT 1
    FROM jsonb_array_elements(
        vd.data -> 'beregningsresultat' -> 'offentligTransport' -> 'reiser'
    ) AS reise
    WHERE reise ->> 'typeAktivitet' IS NULL
      AND reise -> 'perioder' -> 0 -> 'grunnlag' -> 'vedtaksperioder' -> 0 ->> 'typeAktivitet' IS NOT NULL
  );

