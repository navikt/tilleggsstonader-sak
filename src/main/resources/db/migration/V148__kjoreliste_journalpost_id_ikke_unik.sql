DROP INDEX kjoreliste_journalpost_id_idx;
CREATE INDEX idx_kjoreliste_journalpost_id ON kjoreliste (journalpost_id);
