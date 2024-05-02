ALTER TABLE vedtak_tilsyn_barn
    ALTER COLUMN vedtak DROP NOT NULL,
    ADD COLUMN avslag_begrunnelse VARCHAR NULL;

UPDATE vedtak_tilsyn_barn
SET type = 'INNVILGELSE'
WHERE type = 'INNVILGET';