ALTER TABLE fagsak
    ADD COLUMN utbetal_pa_nytt_fagomrade BOOLEAN;

UPDATE fagsak
SET utbetal_pa_nytt_fagomrade = (stonadstype LIKE 'DAGLIG_REISE_%' OR stonadstype LIKE 'REISE_TIL_SAMLING_%')
WHERE utbetal_pa_nytt_fagomrade IS NULL;
