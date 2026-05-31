-- Omdøper feltet "typeAktivitet" til "tiltaksvariant" i database-kolonner og JSONB-strukturer.

-- 1. Rename kolonne vilkar_periode.type_aktivitet -> tiltaksvariant
ALTER TABLE vilkar_periode RENAME COLUMN type_aktivitet TO tiltaksvariant;

-- 2. Rename JSON-nøkkel "typeAktivitet" -> "tiltaksvariant" i vilkar.fakta (OT-vilkår)
UPDATE vilkar
SET fakta = (fakta::jsonb - 'typeAktivitet') || jsonb_build_object('tiltaksvariant', fakta::jsonb -> 'typeAktivitet')
WHERE fakta::jsonb ? 'typeAktivitet';

-- 3. Rename JSON-nøkkel "typeAktivitet" -> "tiltaksvariant" i vedtak.data
--    på stien: beregningsresultat.offentligTransport.reiser[*].typeAktivitet
UPDATE vedtak v
SET data = jsonb_set(
        v.data,
        '{beregningsresultat,offentligTransport,reiser}',
        (SELECT COALESCE(
                        jsonb_agg(
                                CASE
                                    WHEN reise ? 'typeAktivitet'
                                        THEN (reise - 'typeAktivitet') || jsonb_build_object('tiltaksvariant', reise -> 'typeAktivitet')
                                    ELSE reise
                                    END
                        ),
                        '[]'::jsonb
                )
         FROM jsonb_array_elements(
                      v.data -> 'beregningsresultat' -> 'offentligTransport' -> 'reiser'
              ) AS reise),
        false
)
WHERE v.data -> 'beregningsresultat' -> 'offentligTransport' -> 'reiser' IS NOT NULL
  AND EXISTS (SELECT 1
              FROM jsonb_array_elements(
                           v.data -> 'beregningsresultat' -> 'offentligTransport' -> 'reiser'
                   ) AS reise
              WHERE reise ? 'typeAktivitet');
