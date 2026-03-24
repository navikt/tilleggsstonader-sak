ALTER TABLE vilkar_periode ADD COLUMN global_id UUID;
UPDATE vilkar_periode SET global_id = gen_random_uuid();
ALTER TABLE vilkar_periode ALTER COLUMN global_id SET NOT NULL;