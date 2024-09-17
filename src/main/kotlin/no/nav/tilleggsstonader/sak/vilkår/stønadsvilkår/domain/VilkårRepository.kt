package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface VilkårRepository : RepositoryInterface<Vilkår, VilkårId>, InsertUpdateRepository<Vilkår> {

    fun findByBehandlingId(behandlingId: BehandlingId): List<Vilkår>

    @Modifying
    @Query("DELETE from vilkar where behandling_id = :behandlingId")
    fun deleteByBehandlingId(behandlingId: BehandlingId)

    @Modifying
    @Query("UPDATE vilkar SET endret_tid = :nyttTidspunkt WHERE id = :id")
    fun oppdaterEndretTid(id: VilkårId, nyttTidspunkt: LocalDateTime)

    @Modifying
    @Query("UPDATE vilkar SET opprettet_av = 'VL', endret_av = 'VL' WHERE id = :id")
    fun settMaskinelltOpprettet(id: VilkårId)

    fun findByTypeAndBehandlingIdIn(vilkårtype: VilkårType, behandlingIds: Collection<UUID>): List<Vilkår>
}
