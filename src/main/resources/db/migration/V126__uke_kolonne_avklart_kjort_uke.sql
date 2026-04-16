ALTER TABLE avklart_kjort_uke
    ALTER COLUMN ukenummer TYPE VARCHAR;
ALTER TABLE avklart_kjort_uke
    RENAME COLUMN ukenummer TO uke;

UPDATE avklart_kjort_uke
SET uke = EXTRACT(YEAR FROM tom)::TEXT || '-' || uke;
