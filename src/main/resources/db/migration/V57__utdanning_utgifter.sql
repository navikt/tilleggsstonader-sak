UPDATE vilkar_periode vp
SET fakta_og_vurdering = jsonb_set(
        vp.fakta_og_vurdering::jsonb,
        '{vurderinger,harUtgifter}',
        '{
          "svar": "JA",
          "resultat": "OPPFYLT"
        }'::jsonb
                         )
    FROM behandling b
JOIN fagsak f ON f.id = b.fagsak_id
WHERE vp.behandling_id = b.id
  AND f.stonadstype = 'LÆREMIDLER'
  AND vp.type = 'UTDANNING';