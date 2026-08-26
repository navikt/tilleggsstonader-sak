-- Legger til harUtgifter (uten svar) i vurderinger for eksisterende UTDANNING_DAGLIG_REISE_TSO-perioder
-- som mangler feltet etter at det ble lagt til som obligatorisk.
UPDATE vilkar_periode vp
SET fakta_og_vurdering = jsonb_set(
        vp.fakta_og_vurdering::jsonb,
        '{vurderinger,harUtgifter}',
        '{
          "svar": null,
          "resultat": "IKKE_VURDERT"
        }'::jsonb
                         )
    FROM behandling b
JOIN fagsak f ON f.id = b.fagsak_id
WHERE vp.behandling_id = b.id
  AND f.stonadstype = 'DAGLIG_REISE_TSO'
  AND vp.fakta_og_vurdering ->> 'type' = 'UTDANNING_DAGLIG_REISE_TSO'
  AND vp.fakta_og_vurdering -> 'vurderinger' -> 'harUtgifter' IS NULL;
