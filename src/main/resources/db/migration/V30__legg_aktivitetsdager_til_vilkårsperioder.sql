ALTER TABLE vilkar_periode ADD aktivitetsdager integer;

UPDATE vilkar_periode
SET aktivitetsdager = 5
WHERE type in ('TILTAK', 'UTDANNING', 'REELL_ARBEIDSSØKER');