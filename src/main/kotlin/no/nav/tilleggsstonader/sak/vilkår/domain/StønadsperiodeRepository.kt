package no.nav.tilleggsstonader.sak.vilkår.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VilkårperiodeRepository : RepositoryInterface<Vilkårperiode, UUID>, InsertUpdateRepository<Vilkårperiode> {

    @Query("SELECT * FROM vilkar_periode WHERE vilkar_id IN (SELECT id FROM vilkar WHERE behandling_id = :behandlingId)")
    fun finnVilkårperioderForBehandling(behandlingId: UUID): List<Vilkårperiode>
}
