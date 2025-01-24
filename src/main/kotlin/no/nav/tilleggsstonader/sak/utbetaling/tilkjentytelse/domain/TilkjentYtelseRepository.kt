package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TilkjentYtelseRepository : RepositoryInterface<TilkjentYtelse, UUID>, InsertUpdateRepository<TilkjentYtelse> {

    fun findByBehandlingId(behandlingId: BehandlingId): TilkjentYtelse?

    @Query("SELECT * FROM tilkjent_ytelse WHERE behandling_id = :behandlingId FOR UPDATE OF tilkjent_ytelse")
    fun findByBehandlingIdForUpdate(behandlingId: BehandlingId): TilkjentYtelse?
}
