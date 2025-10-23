UPDATE vilkar
SET fakta = json_build_object(
        'typeVilkårFakta', 'DAGLIG_REISE_OFFENTLIG_TRANSPORT',
        'reisedagerPerUke', offentlig_transport_reisedager_per_uke,
        'prisEnkelbillett', offentlig_transport_pris_enkelbillett,
        'prisSyvdagersbillett', offentlig_transport_pris_syvdagersbillett,
        'prisTrettidagersbillett', offentlig_transport_pris_trettidagersbillett
            )
WHERE offentlig_transport_reisedager_per_uke IS NOT NULL
   OR offentlig_transport_pris_enkelbillett IS NOT NULL
   OR offentlig_transport_pris_syvdagersbillett IS NOT NULL
   OR offentlig_transport_pris_trettidagersbillett IS NOT NULL;

ALTER TABLE vilkar
    DROP COLUMN offentlig_transport_reisedager_per_uke,
    DROP COLUMN offentlig_transport_pris_enkelbillett,
    DROP COLUMN offentlig_transport_pris_syvdagersbillett,
    DROP COLUMN offentlig_transport_pris_trettidagersbillett;