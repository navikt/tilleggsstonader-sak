
ALTER TABLE vedtak
     ADD COLUMN status varchar DEFAULT 'NY';

ALTER TABLE vedtak
    ALTER COLUMN status SET NOT NULL ;
