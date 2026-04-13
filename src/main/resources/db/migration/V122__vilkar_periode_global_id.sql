ALTER TABLE vilkar_periode ADD COLUMN global_id UUID;

WITH RECURSIVE chain AS (SELECT id, gen_random_uuid() AS root_id
                         FROM vilkar_periode
                         WHERE forrige_vilkarperiode_id IS NULL
                         UNION ALL
                         SELECT vp.id, c.root_id
                         FROM vilkar_periode vp
                                  JOIN chain c ON vp.forrige_vilkarperiode_id = c.id)
UPDATE vilkar_periode vp
SET global_id = c.root_id
FROM chain c
WHERE c.id = vp.id
  AND vp.global_id IS NULL;

ALTER TABLE vilkar_periode ALTER COLUMN global_id SET NOT NULL;