UPDATE vilkarperioder_grunnlag
SET grunnlag = jsonb_set(
        grunnlag::jsonb,
        '{ytelse,kildeResultat}',
        '[
          {
            "type": "AAP",
            "resultat": "OK"
          },
          {
            "type": "ENSLIG_FORSØRGER",
            "resultat": "OK"
          },
          {
            "type": "OMSTILLINGSSTØNAD",
            "resultat": "OK"
          }
        ]'::jsonb)::json
WHERE grunnlag -> 'ytelse' IS NOT NULL;