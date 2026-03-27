ALTER TABLE vilkar_periode ADD COLUMN global_id UUID;

WITH RECURSIVE chain AS (
    SELECT id, gen_random_uuid() AS root_id FROM vilkar_periode WHERE forrige_vilkarperiode_id IS NULL
    UNION ALL
    SELECT vp.id, c.root_id FROM vilkar_periode vp JOIN chain c ON vp.forrige_vilkarperiode_id = c.id
)
UPDATE vilkar_periode SET global_id = (SELECT root_id FROM chain WHERE chain.id = vilkar_periode.id);

ALTER TABLE vilkar_periode ALTER COLUMN global_id SET NOT NULL;