UPDATE andel_tilkjent_ytelse
SET status_iverksetting = 'UAKTUELL'
WHERE tilkjent_ytelse_id in (select ty.id
                             from tilkjent_ytelse ty
                                      join behandling b on ty.behandling_id = b.id
                             where b.resultat = 'HENLAGT'
                               and b.status = 'FERDIGSTILT');