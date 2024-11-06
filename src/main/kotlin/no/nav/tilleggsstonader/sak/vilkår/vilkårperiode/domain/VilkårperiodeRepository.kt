package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VilkårperiodeRepository : RepositoryInterface<MålgruppeEllerAktivitet, UUID>, InsertUpdateRepository<MålgruppeEllerAktivitet> {

    fun findByBehandlingId(behandlingId: BehandlingId): List<MålgruppeEllerAktivitet>

    fun findByBehandlingIdAndResultat(behandlingId: BehandlingId, resultat: ResultatVilkårperiode): List<MålgruppeEllerAktivitet>

    fun findByBehandlingIdAndResultatNot(behandlingId: BehandlingId, resultat: ResultatVilkårperiode): List<MålgruppeEllerAktivitet>
}
