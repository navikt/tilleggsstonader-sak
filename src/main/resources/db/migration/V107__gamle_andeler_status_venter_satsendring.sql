UPDATE andel_tilkjent_ytelse
SET status_iverksetting = 'UAKTUELL'
WHERE status_iverksetting = 'VENTER_PÅ_SATS_ENDRING'
  AND tilkjent_ytelse_id in (SELECT ty.id
                             FROM behandling b
                                      JOIN tilkjent_ytelse ty ON b.id = ty.behandling_id
                                      JOIN andel_tilkjent_ytelse aty ON ty.id = aty.tilkjent_ytelse_id
                             WHERE aty.status_iverksetting = 'VENTER_PÅ_SATS_ENDRING'
                               AND b.resultat IN ('OPPHØRT', 'INNVILGET')
                               AND b.id NOT IN (SELECT gib.id FROM gjeldende_iverksatte_behandlinger gib));
