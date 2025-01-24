/**
  Erstatter utbetalingsmåned med utbetalingsdato.
  Setter utbetalingsdato til første ukedag i måneden då det er det dato som tidligere blitt brukt for å opprette andeler
 */
UPDATE vedtak
SET data = jsonb_set(
        data,
        '{beregningsresultat,perioder}',
        (SELECT jsonb_agg(
                        jsonb_set(
                                periode #- '{grunnlag,utbetalingsmåned}', -- Sletter utbetalingsmåned,
                                '{grunnlag,utbetalingsdato}', -- Legger til utbetalingsdato
                        -- Setter utbetalingsdato til dato eller neste mandagen hvis helg
                                to_jsonb(
                                        (SELECT CASE
                                                    WHEN EXTRACT(DOW FROM first_day_of_month) = 0
                                                        THEN first_day_of_month + INTERVAL '1 day' -- Søndag -> Mandag
                                                    WHEN EXTRACT(DOW FROM first_day_of_month) = 6
                                                        THEN first_day_of_month + INTERVAL '2 days' -- Lørdag -> Mandag
                                                    ELSE first_day_of_month
                                                    END)::date
                                )
                        )
                )
         FROM jsonb_array_elements(data -> 'beregningsresultat' -> 'perioder') AS periode,
              LATERAL (
                       SELECT to_date(periode -> 'grunnlag' ->> 'utbetalingsmåned', 'YYYY-MM') AS first_day_of_month
                  ))
    , true -- legger til element hvis det mangler
           )
where data ->> 'type' = 'INNVILGELSE_LÆREMIDLER'
  AND data::text LIKE '%utbetalingsmåned%'
;
