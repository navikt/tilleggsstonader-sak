ALTER TABLE fagsak_utbetaling_id ADD COLUMN reise_id UUID;
ALTER TABLE andel_tilkjent_ytelse ADD COLUMN reise_id UUID;

DROP INDEX fagsak_utbetaling_id_fagsak_id_type_andel_idx;
CREATE UNIQUE INDEX ON FAGSAK_UTBETALING_ID (fagsak_id, type_andel, reise_id);
