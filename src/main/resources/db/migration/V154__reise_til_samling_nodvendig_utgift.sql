UPDATE vilkar
SET fakta = jsonb_set(
        fakta::jsonb,
        '{nødvendigUtgift}',
        jsonb_build_object(
                'erUtgiftenNødvendig', 'GAMMEL_MANGLER_DATA',
                'begrunnelse', null
        ),
        true
            )
WHERE type = 'REISE_TIL_SAMLING'
  AND fakta IS NOT NULL
  AND fakta::jsonb ->> 'type' IN ('REISE_TIL_SAMLING_OFFENTLIG_TRANSPORT', 'REISE_TIL_SAMLING_PRIVAT_BIL')
  AND fakta::jsonb -> 'nødvendigUtgift' IS NULL;
