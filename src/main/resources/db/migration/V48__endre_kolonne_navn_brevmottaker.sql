ALTER TABLE brevmottaker RENAME COLUMN navn_hos_organisasjon TO mottaker_navn;
ALTER TABLE brevmottaker ADD COLUMN organisasjons_navn VARCHAR;