ALTER TABLE simuleringsresultat ADD COLUMN ingen_endring_i_utbetaling BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE simuleringsresultat ALTER COLUMN data DROP NOT NULL;