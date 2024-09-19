ALTER TABLE vilkar_periode
    ADD COLUMN status VARCHAR;
ALTER TABLE vilkar
    ADD COLUMN status VARCHAR;

----------------
-- Vilkårperiode
----------------
UPDATE vilkar_periode
SET status = 'NY';

-- Oppdaterer vilkårperiode til UENDRET, for å senere oppdatere de som har endringer til ENDRET
UPDATE vilkar_periode vp
SET status = 'UENDRET'
FROM vilkar_periode vp_gammel
WHERE vp.forrige_vilkarperiode_id = vp_gammel.id
  AND vp.type = vp_gammel.type
  AND vp.fom = vp_gammel.fom
  AND vp.tom = vp_gammel.tom
  AND vp.resultat = vp_gammel.resultat
  AND vp.aktivitetsdager is not distinct from vp_gammel.aktivitetsdager
  AND vp.delvilkar::text = vp_gammel.delvilkar::text
  AND vp.resultat <> 'SLETTET'
;

-- Oppdaterer vilkårperiode til ENDRET dersom det finnes endringer fra forrige vilkår
UPDATE vilkar_periode vp
SET status = 'ENDRET'
FROM vilkar_periode vp_gammel
WHERE vp.forrige_vilkarperiode_id = vp_gammel.id
  AND NOT (
    vp.type = vp_gammel.type
        AND vp.fom = vp_gammel.fom
        AND vp.tom = vp_gammel.tom
        AND vp.resultat = vp_gammel.resultat
        AND vp.aktivitetsdager is not distinct from vp_gammel.aktivitetsdager
        AND vp.delvilkar::text = vp_gammel.delvilkar::text
    )
  AND vp.resultat <> 'SLETTET'
;

UPDATE vilkar_periode
SET status = 'SLETTET'
WHERE resultat = 'SLETTET';
ALTER TABLE vilkar_periode
    ALTER COLUMN status SET NOT NULL;

----------------
-- Vilkår
----------------
UPDATE vilkar
SET status = 'NY';

-- Oppdaterer vilkår til UENDRET hvis de har opphavsvilkår, for å senere oppdatere de som er endret til endret
UPDATE vilkar vp
SET status = 'UENDRET'
FROM behandling b
WHERE vp.behandling_id = b.id
  AND vp.opphavsvilkaar_behandling_id IS NOT NULL
;

-- Oppdaterer de vilkår som har opphavsvilkår men mangler match i forrige behandling, til status ENDRET
UPDATE vilkar vp
SET status = 'ENDRET'
FROM behandling b
WHERE b.id = vp.behandling_id
  AND vp.opphavsvilkaar_behandling_id IS NOT NULL
  AND b.forrige_behandling_id IS NOT NULL
  AND NOT EXISTS (SELECT *
                  FROM behandling b
                           JOIN behandling_barn barn ON vp.barn_id::uuid = barn.id
                           JOIN vilkar vilkar_forrige on vilkar_forrige.behandling_id = b.forrige_behandling_id
                           JOIN behandling_barn barn_forrige
                                ON vilkar_forrige.barn_id::uuid = barn_forrige.id AND barn.ident = barn_forrige.ident
                  WHERE vilkar_forrige.behandling_id = b.forrige_behandling_id
                    AND vilkar_forrige.fom is not distinct from vp.fom
                    AND vilkar_forrige.tom is not distinct from vp.tom
                    AND vilkar_forrige.utgift is not distinct from vp.utgift
                    AND vilkar_forrige.delvilkar::text = vp.delvilkar::text)
;

ALTER TABLE vilkar
    ALTER COLUMN status SET NOT NULL;