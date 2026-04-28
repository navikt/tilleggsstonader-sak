ALTER TABLE behandling
    ADD COLUMN behandling_metode VARCHAR;

UPDATE behandling
SET behandling_metode = 'BATCH'
WHERE behandling.arsak = 'SATSENDRING';

UPDATE behandling
SET behandling_metode = 'MANUELL'
WHERE behandling_metode IS NULL;

ALTER TABLE behandling
    ALTER COLUMN behandling_metode SET NOT NULL;