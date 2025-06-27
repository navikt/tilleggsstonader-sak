-- Drop eksisterende constraint
ALTER TABLE fakta_grunnlag DROP CONSTRAINT unique_behandling_type;

-- Legg til unik index som behandler NULL type_id som en verdi
-- dette gir unike verdier selv om type_id skulle være NULL
CREATE UNIQUE INDEX unique_behandling_type_idx ON fakta_grunnlag
    (behandling_id, type, COALESCE(type_id, 'NULL_VALUE'));