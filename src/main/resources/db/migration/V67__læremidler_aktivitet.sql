WITH stønadsperioderForLæremidler
         AS (SELECT s.behandling_id, s.aktivitet
             FROM stonadsperiode s
                      JOIN behandling b ON b.id = s.behandling_id
                      JOIN fagsak f ON b.fagsak_id = f.id
             WHERE f.stonadstype = 'LÆREMIDLER'
             GROUP BY s.behandling_id, s.aktivitet),
     behandlinger_med_1_aktivitetstype
         AS (SELECT behandling_id
             FROM stønadsperioderForLæremidler q
             GROUP BY behandling_id
             HAVING count(*) = 1),
     behandling_id_og_aktivitet
         AS (SELECT q1.behandling_id, q2.aktivitet
             FROM behandlinger_med_1_aktivitetstype q1
                      JOIN stønadsperioderForLæremidler q2 ON q2.behandling_id = q1.behandling_id)
UPDATE vedtak v
SET data =
        jsonb_set(
                v.data,
                '{beregningsresultat,perioder}',
                (SELECT jsonb_agg(
                                jsonb_set(
                                        periode,
                                        '{grunnlag,aktivitet}',
                                        to_jsonb(ba.aktivitet)
                                )
                        )
                 FROM jsonb_array_elements(v.data -> 'beregningsresultat' -> 'perioder') periode)
        )
FROM behandling_id_og_aktivitet ba
WHERE v.behandling_id = ba.behandling_id
  AND v.type IN ('INNVILGELSE', 'OPPHØR');