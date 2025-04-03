UPDATE vedtak
SET data =
        replace(
                replace(
                        replace(
                                replace(data::text, '"AAP"', '"NEDSATT_ARBEIDSEVNE"'),
                                '"OVERGANGSSTØNAD"', '"ENSLIG_FORSØRGER"'
                        ), '"OMSTILLINGSSTØNAD"', '"GJENLEVENDE"'
                ), '"UFØRETRYGD"', '"NEDSATT_ARBEIDSEVNE"'
        )::json
WHERE data ->> 'type' IN ('INNVILGELSE_TILSYN_BARN', 'OPPHØR_TILSYN_BARN', 'INNVILGELSE_BOUTGIFTER');