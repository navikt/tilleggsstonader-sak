ALTER TABLE oppgave ADD COLUMN status VARCHAR(50);
UPDATE oppgave SET status='ÅPEN' WHERE er_ferdigstilt = false;
UPDATE oppgave SET status='FERDIGSTILT' WHERE er_ferdigstilt = true;
ALTER TABLE oppgave DROP COLUMN er_ferdigstilt;

ALTER TABLE oppgave ADD COLUMN tildelt_enhetsnummer VARCHAR(10);
ALTER TABLE oppgave ADD COLUMN enhetsmappe_id INTEGER;
