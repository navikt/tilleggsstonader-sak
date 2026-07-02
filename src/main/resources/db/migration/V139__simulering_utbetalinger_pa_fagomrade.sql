ALTER TABLE simuleringsresultat
    ADD COLUMN finnes_ikke_registrert_utbetalinger_fagomrade BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE simuleringsresultat ALTER COLUMN finnes_ikke_registrert_utbetalinger_fagomrade DROP DEFAULT;
