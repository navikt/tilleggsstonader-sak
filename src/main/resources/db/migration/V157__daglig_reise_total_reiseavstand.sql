-- Ved rollback må også nye kilometerverdier konverteres tilbake før gammel sak startes.
CREATE FUNCTION pg_temp.konverter_total_reiseavstand(grunnlag JSONB)
    RETURNS JSONB
    LANGUAGE plpgsql
AS
$$
DECLARE
    avstand NUMERIC;
BEGIN
    IF jsonb_typeof(grunnlag) IS DISTINCT FROM 'object' THEN
        RAISE EXCEPTION 'Privatbilgrunnlag mangler eller er ugyldig';
    END IF;

    IF grunnlag ? 'reiseavstandEnVei' AND grunnlag ? 'totalReiseavstand' THEN
        RAISE EXCEPTION 'Privatbilgrunnlag har begge avstandsfeltene';
    END IF;

    IF grunnlag ? 'reiseavstandEnVei' THEN
        IF jsonb_typeof(grunnlag -> 'reiseavstandEnVei') IS DISTINCT FROM 'number' THEN
            RAISE EXCEPTION 'Privatbilgrunnlag har ugyldig enveisavstand';
        END IF;
        avstand := (grunnlag ->> 'reiseavstandEnVei')::NUMERIC * 2;
    ELSE
        IF jsonb_typeof(grunnlag -> 'totalReiseavstand') IS DISTINCT FROM 'number' THEN
            RAISE EXCEPTION 'Privatbilgrunnlag mangler gyldig totalavstand';
        END IF;
        avstand := (grunnlag ->> 'totalReiseavstand')::NUMERIC;
    END IF;

    IF avstand <= 0 THEN
        RAISE EXCEPTION 'Privatbilgrunnlag har ikke positiv avstand';
    END IF;

    RETURN (grunnlag - 'reiseavstandEnVei') ||
           jsonb_build_object('totalReiseavstand', trim_scale(avstand));
END;
$$;

DO
$$
DECLARE
    antall_vilkar INTEGER;
    antall_vedtak INTEGER;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM vedtak
        WHERE data ->> 'type' IN ('INNVILGELSE_DAGLIG_REISE', 'OPPHØR_DAGLIG_REISE')
          AND data -> 'rammevedtakPrivatBil' IS NOT NULL
          AND data -> 'rammevedtakPrivatBil' <> 'null'::JSONB
          AND jsonb_typeof(data #> '{rammevedtakPrivatBil,reiser}') IS DISTINCT FROM 'array'
    ) THEN
        RAISE EXCEPTION 'Rammevedtak for privat bil mangler gyldig reiseliste';
    END IF;

    UPDATE vilkar
    SET fakta = pg_temp.konverter_total_reiseavstand(fakta::JSONB)::JSON
    WHERE fakta ->> 'type' = 'DAGLIG_REISE_PRIVAT_BIL';
    GET DIAGNOSTICS antall_vilkar = ROW_COUNT;

    UPDATE vedtak
    SET data = jsonb_set(
        data,
        '{rammevedtakPrivatBil,reiser}',
        (
            SELECT jsonb_agg(
                jsonb_set(reise, '{grunnlag}', pg_temp.konverter_total_reiseavstand(reise -> 'grunnlag'))
                ORDER BY rekkefolge
            )
            FROM jsonb_array_elements(data #> '{rammevedtakPrivatBil,reiser}')
                WITH ORDINALITY AS reiser(reise, rekkefolge)
        )
    )
    WHERE data ->> 'type' IN ('INNVILGELSE_DAGLIG_REISE', 'OPPHØR_DAGLIG_REISE')
      AND jsonb_array_length(
          COALESCE(NULLIF(data #> '{rammevedtakPrivatBil,reiser}', 'null'::JSONB), '[]'::JSONB)
      ) > 0;
    GET DIAGNOSTICS antall_vedtak = ROW_COUNT;

    RAISE NOTICE 'Konvertert total reiseavstand i % vilkar og % vedtak', antall_vilkar, antall_vedtak;
END;
$$;

DROP FUNCTION pg_temp.konverter_total_reiseavstand(JSONB);
