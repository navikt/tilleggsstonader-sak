ALTER TABLE kjoreliste
    ADD COLUMN begrunnelse VARCHAR NULL,
    ADD COLUMN behandling_id UUID NULL REFERENCES behandling (id),
    ADD COLUMN manuelt_registrert BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE kjoreliste ALTER COLUMN manuelt_registrert DROP DEFAULT;