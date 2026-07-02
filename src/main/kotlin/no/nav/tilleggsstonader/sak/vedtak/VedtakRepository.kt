package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository

@Repository
interface VedtakRepository :
    RepositoryInterface<Vedtak, BehandlingId>,
    InsertUpdateRepository<Vedtak> {
    @Query(
        """
        select exists(
            select 1 from vedtak v
            where v.behandling_id in (:behandlingIder)
            and jsonb_array_length(v.data -> 'rammevedtakPrivatBil' -> 'reiser') > 0
        )
        """,
    )
    fun harRammevedtak(behandlingIder: List<BehandlingId>): Boolean
}
