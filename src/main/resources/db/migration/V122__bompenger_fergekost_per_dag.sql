UPDATE vedtak
SET data = replace(data::text, '"bompengerEnVei":', '"bompengerPerDag":')::jsonb
WHERE data ->> 'type' = 'INNVILGELSE_DAGLIG_REISE'
  AND jsonb_path_exists(data, '$.rammevedtakPrivatBil')
  AND jsonb_array_length(data -> 'rammevedtakPrivatBil' -> 'reiser') > 0;

UPDATE vedtak
SET data = replace(data::text, '"fergekostnadEnVei":', '"fergekostnadPerDag":')::jsonb
WHERE data ->> 'type' = 'INNVILGELSE_DAGLIG_REISE'
  AND jsonb_path_exists(data, '$.rammevedtakPrivatBil')
  AND jsonb_array_length(data -> 'rammevedtakPrivatBil' -> 'reiser') > 0;
