ALTER TABLE vedtak_tilsyn_barn
    ADD COLUMN opphor_begrunnelse VARCHAR,
    ADD COLUMN arsaker_opphor JSON;

ALTER TABLE vedtaksstatistikk
    ADD COLUMN arsaker_opphor JSON;