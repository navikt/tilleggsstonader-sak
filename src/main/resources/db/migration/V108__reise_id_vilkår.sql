-- Oppretter reiseId på vilkår->fakta for daglig reise offentlig transport
UPDATE vilkar
SET fakta = jsonb_set(
        fakta::jsonb,
        '{reiseId}',
        to_jsonb(gen_random_uuid()),
        true
            )
WHERE fakta IS NOT NULL
  AND type = 'DAGLIG_REISE';
