ALTER TABLE kjoreliste
    ADD COLUMN begrunnelse VARCHAR NULL,
    ADD COLUMN manuelt_lagret_i_behandling UUID NULL REFERENCES behandling (id);

CREATE INDEX idx_kjoreliste_manuelt_lagret_i_behandling ON kjoreliste (manuelt_lagret_i_behandling) WHERE manuelt_lagret_i_behandling IS NOT NULL;