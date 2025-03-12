UPDATE vilkar_periode
SET fakta_og_vurdering = jsonb_set(fakta_og_vurdering, '{vurderinger, aldersvilkår}', '{
  "svar": "GAMMEL_MANGLER_DATA",
  "resultat": "IKKE_VURDERT",
  "vurderingFaktaEtterlevelse": null
}'::jsonb)
WHERE type IN ('AAP', 'NEDSATT_ARBEIDSEVNE', 'UFØRETRYGD', 'OMSTILLINGSSTØNAD');
