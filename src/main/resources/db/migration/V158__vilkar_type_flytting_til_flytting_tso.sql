-- VilkårType FLYTTING er delt i FLYTTING_TSO og FLYTTING_TSR.
-- Vilkår på flyttebehandlinger for TSR finnes kun i dev, men oppdateres slik at de får riktig type.
UPDATE vilkar v
SET type = CASE f.stonadstype
               WHEN 'FLYTTING_TSR' THEN 'FLYTTING_TSR'
               ELSE 'FLYTTING_TSO'
           END
FROM behandling b
         JOIN fagsak f ON f.id = b.fagsak_id
WHERE v.behandling_id = b.id
  AND v.type = 'FLYTTING';
