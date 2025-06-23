UPDATE vedtak
SET data = regexp_replace(
        data::text,
        '("utgift"\s*:\s*(\d+))',
        '\1, "skalFåDekketFaktiskeUtgifter": false',
        'g'
           )::jsonb
WHERE data ->> 'type' in ('INNVILGELSE_BOUTGIFTER', 'OPPHØR_BOUTGIFTER')
  AND data::text LIKE '%"utgift"%'
;