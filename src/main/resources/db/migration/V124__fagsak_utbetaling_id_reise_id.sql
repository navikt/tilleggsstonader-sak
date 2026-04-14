ALTER TABLE fagsak_utbetaling_id ADD COLUMN reise_id UUID;
ALTER TABLE andel_tilkjent_ytelse ADD COLUMN reise_id UUID;

DROP INDEX fagsak_utbetaling_id_fagsak_id_type_andel_idx;

CREATE UNIQUE INDEX fagsak_utbetaling_id_uq_fagsak_type_when_reise_null
    ON fagsak_utbetaling_id (fagsak_id, type_andel)
    WHERE reise_id IS NULL;

CREATE UNIQUE INDEX fagsak_utbetaling_id_uq_fagsak_type_reise_when_reise_not_null
    ON fagsak_utbetaling_id (fagsak_id, type_andel, reise_id)
    WHERE reise_id IS NOT NULL;