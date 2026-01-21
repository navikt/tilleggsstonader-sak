-- Flytt adresse fra vilkår inn i fakta der fakta eksisterer fra før
UPDATE vilkar
SET fakta = jsonb_set(
        fakta::jsonb,
        '{adresse}',
        to_jsonb(adresse),
        true
            )
WHERE type = 'DAGLIG_REISE'
  AND fakta IS NOT NULL
  AND adresse IS NOT NULL;

-- Opprett fakta der det mangler (adresse kan være NULL)
UPDATE vilkar
SET fakta = jsonb_build_object(
        'type', 'DAGLIG_REISE_UBESTEMT',
        'reiseId', gen_random_uuid(),
        'adresse', adresse
            )
WHERE type = 'DAGLIG_REISE'
  AND fakta IS NULL;

-- Dropp kolonnen nå som adresse er overført til fakta
ALTER TABLE vilkar DROP COLUMN IF EXISTS adresse;