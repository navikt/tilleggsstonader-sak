CREATE TABLE avklart_kjort_uke_avvik
(
    id                   UUID    NOT NULL PRIMARY KEY,
    avklart_kjort_uke_id UUID    NOT NULL REFERENCES avklart_kjort_uke (id),
    type_avvik           VARCHAR NOT NULL
);

CREATE INDEX idx_avklart_kjort_uke_avvik_avklart_kjort_uke_id ON avklart_kjort_uke_avvik (avklart_kjort_uke_id);

INSERT INTO avklart_kjort_uke_avvik (id, avklart_kjort_uke_id, type_avvik)
SELECT gen_random_uuid(), id, type_avvik
FROM avklart_kjort_uke
WHERE type_avvik IS NOT NULL;

ALTER TABLE avklart_kjort_uke
    DROP COLUMN type_avvik;
