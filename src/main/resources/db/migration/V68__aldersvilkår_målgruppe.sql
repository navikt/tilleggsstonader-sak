UPDATE vilkar_periode
SET fakta_og_vurdering = jsonb_set(fakta_og_vurdering, '{vurderinger, aldersvilkår}', '{
"svar": "GAMMEL_MANGLER_DATA",
"resultat": "OPPFYLT",
"vurderingFaktaEtterlevelse": null
}');