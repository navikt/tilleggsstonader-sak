ALTER TABLE stonadsperiode
    ADD COLUMN status VARCHAR;

UPDATE stonadsperiode
SET status='NY';

ALTER TABLE stonadsperiode
    ALTER COLUMN status SET NOT NULL;