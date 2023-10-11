package no.nav.tilleggsstonader.sak.vilkår.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface VilkårRepository : RepositoryInterface<Vilkår, UUID>, InsertUpdateRepository<Vilkår> {

    fun findByBehandlingId(behandlingId: UUID): List<Vilkår>

    @Modifying
    @Query("DELETE from vilkar where behandling_id = :behandlingId")
    fun deleteByBehandlingId(behandlingId: UUID)

    @Modifying
    @Query("UPDATE vilkar SET endret_tid = :nyttTidspunkt WHERE id = :id")
    fun oppdaterEndretTid(id: UUID, nyttTidspunkt: LocalDateTime)

    @Modifying
    @Query("UPDATE vilkar SET opprettet_av = 'VL', endret_av = 'VL' WHERE id = :id")
    fun settMaskinelltOpprettet(id: UUID)

    fun findByTypeAndBehandlingIdIn(vilkårtype: VilkårType, behandlingIds: Collection<UUID>): List<Vilkår>
}
