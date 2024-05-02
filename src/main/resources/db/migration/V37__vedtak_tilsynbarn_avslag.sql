ALTER TABLE vedtak_tilsyn_barn
    ALTER
        COLUMN vedtak DROP
        NOT NULL,
    ADD COLUMN avslag_begrunnelse VARCHAR NULL;