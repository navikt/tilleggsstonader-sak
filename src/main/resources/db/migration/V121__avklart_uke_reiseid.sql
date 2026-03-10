ALTER TABLE avklart_kjort_uke
    ADD COLUMN reise_id UUID NULL;

UPDATE avklart_kjort_uke
SET reise_id = (select (data ->> 'reiseId')::UUID from kjoreliste where kjoreliste.id = avklart_kjort_uke.kjoreliste_id);

ALTER TABLE avklart_kjort_uke
    ALTER COLUMN reise_id SET NOT NULL;