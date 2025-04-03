UPDATE vilkar_periode
SET fakta_og_vurdering = jsonb_set(fakta_og_vurdering, '{vurderinger, mottarSykepengerForFulltidsstilling}', '{
  "svar": "GAMMEL_MANGLER_DATA",
  "resultat": "IKKE_VURDERT"
}'::jsonb)
WHERE type IN ('NEDSATT_ARBEIDSEVNE')
  and behandling_id in
      (select b.id
       from behandling b
                join fagsak f on b.fagsak_id = f.id
       where f.stonadstype != 'LÆREMIDLER');

UPDATE vilkar_periode
SET fakta_og_vurdering = jsonb_set(fakta_og_vurdering, '{vurderinger, mottarSykepengerForFulltidsstilling}', '{
  "svar": "NEI_IMPLISITT",
  "resultat": "OPPFYLT"
}'::jsonb)
WHERE type IN ('AAP', 'UFØRETRYGD')
  and behandling_id in
      (select b.id
       from behandling b
                join fagsak f on b.fagsak_id = f.id
       where f.stonadstype != 'LÆREMIDLER');
