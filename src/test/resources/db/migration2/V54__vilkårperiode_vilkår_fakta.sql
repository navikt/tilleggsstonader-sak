ALTER TABLE vilkar_periode
    ADD COLUMN fakta_og_vurdering JSONB;

-- TODO Skal vi beholde vurderinger som settes til IKKE_VURDERT? Som ikke skal vurderes... ?

-- noinspection SqlWithoutWhere
UPDATE vilkar_periode
SET fakta_og_vurdering =
        jsonb_build_object(
                'type', type || '_TILSYN_BARN',
                'vurderinger', delvilkar::jsonb - '@type',
                'fakta',
                CASE
                    WHEN aktivitetsdager IS NOT NULL THEN jsonb_build_object('aktivitetsdager', aktivitetsdager)
                    ELSE '{}'::jsonb
                    END
        );

ALTER TABLE vilkar_periode
    DROP COLUMN delvilkar;
ALTER TABLE vilkar_periode
    ALTER COLUMN fakta_og_vurdering SET NOT NULL;

/**
TILTAK,"{""type"": ""TILTAK_TILSYN_BARN"", ""fakta"": {""aktivitetsdager"": 5}, ""vurderinger"": {""lønnet"": {""svar"": ""NEI"", ""resultat"": ""OPPFYLT""}}}"
UTDANNING,"{""type"": ""UTDANNING_TILSYN_BARN"", ""fakta"": {""aktivitetsdager"": 5}, ""vurderinger"": {""lønnet"": {""svar"": null, ""resultat"": ""IKKE_AKTUELT""}}}"
REELL_ARBEIDSSØKER,"{""type"": ""REELL_ARBEIDSSØKER_TILSYN_BARN"", ""fakta"": {""aktivitetsdager"": 5}, ""vurderinger"": {""lønnet"": {""svar"": null, ""resultat"": ""IKKE_AKTUELT""}}}"
INGEN_AKTIVITET,"{""type"": ""INGEN_AKTIVITET_TILSYN_BARN"", ""fakta"": {}, ""vurderinger"": {""lønnet"": {""svar"": null, ""resultat"": ""IKKE_AKTUELT""}}}"
AAP,"{""type"": ""AAP_TILSYN_BARN"", ""fakta"": {}, ""vurderinger"": {""medlemskap"": {""svar"": ""JA_IMPLISITT"", ""resultat"": ""OPPFYLT""}, ""dekketAvAnnetRegelverk"": {""svar"": ""NEI"", ""resultat"": ""OPPFYLT""}}}"
UFØRETRYGD,"{""type"": ""UFØRETRYGD_TILSYN_BARN"", ""fakta"": {}, ""vurderinger"": {""medlemskap"": {""svar"": ""NEI"", ""resultat"": ""IKKE_OPPFYLT""}, ""dekketAvAnnetRegelverk"": {""svar"": ""NEI"", ""resultat"": ""OPPFYLT""}}}"
OMSTILLINGSSTØNAD,"{""type"": ""OMSTILLINGSSTØNAD_TILSYN_BARN"", ""fakta"": {}, ""vurderinger"": {""medlemskap"": {""svar"": ""JA"", ""resultat"": ""OPPFYLT""}, ""dekketAvAnnetRegelverk"": {""svar"": null, ""resultat"": ""IKKE_AKTUELT""}}}"
OVERGANGSSTØNAD,"{""type"": ""OVERGANGSSTØNAD_TILSYN_BARN"", ""fakta"": {}, ""vurderinger"": {""medlemskap"": {""svar"": ""JA_IMPLISITT"", ""resultat"": ""OPPFYLT""}, ""dekketAvAnnetRegelverk"": {""svar"": null, ""resultat"": ""IKKE_AKTUELT""}}}"
NEDSATT_ARBEIDSEVNE,"{""type"": ""NEDSATT_ARBEIDSEVNE_TILSYN_BARN"", ""fakta"": {}, ""vurderinger"": {""medlemskap"": {""svar"": ""JA"", ""resultat"": ""OPPFYLT""}, ""dekketAvAnnetRegelverk"": {""svar"": ""NEI"", ""resultat"": ""OPPFYLT""}}}"
SYKEPENGER_100_PROSENT,"{""type"": ""SYKEPENGER_100_PROSENT_TILSYN_BARN"", ""fakta"": {}, ""vurderinger"": {""medlemskap"": {""svar"": null, ""resultat"": ""IKKE_AKTUELT""}, ""dekketAvAnnetRegelverk"": {""svar"": null, ""resultat"": ""IKKE_AKTUELT""}}}"
INGEN_MÅLGRUPPE,"{""type"": ""INGEN_MÅLGRUPPE_TILSYN_BARN"", ""fakta"": {}, ""vurderinger"": {""medlemskap"": {""svar"": null, ""resultat"": ""IKKE_AKTUELT""}, ""dekketAvAnnetRegelverk"": {""svar"": null, ""resultat"": ""IKKE_AKTUELT""}}}"

 */
