UPDATE vedtak
SET data = replace(data::text, 'AAP', 'NEDSATT_ARBEIDSEVNE')::jsonb
WHERE type IN ('INNVILGELSE', 'OPPHØR');
UPDATE vedtak
SET data = replace(data::text, 'NEDSATT_ARBEIDSEVNE', 'NEDSATT_ARBEIDSEVNE')::jsonb
WHERE type IN ('INNVILGELSE', 'OPPHØR');
UPDATE vedtak
SET data = replace(data::text, 'UFØRETRYGD', 'NEDSATT_ARBEIDSEVNE')::jsonb
WHERE type IN ('INNVILGELSE', 'OPPHØR');

UPDATE vedtak
SET data = replace(data::text, 'OMSTILLINGSSTØNAD', 'GJENLEVENDE')::jsonb
WHERE type IN ('INNVILGELSE', 'OPPHØR');

UPDATE vedtak
SET data = replace(data::text, 'OVERGANGSSTØNAD', 'ENSLIG_FORSØRGER')::jsonb
WHERE type IN ('INNVILGELSE', 'OPPHØR');