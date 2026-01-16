package no.nav.tilleggsstonader.sak.integrasjonstest.dsl

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import java.time.LocalDate

@BehandlingTestdataDslMarker
class StønadsvilkårTestdataDsl {
    internal val opprettScope = OpprettStønadsvilkårDsl()
    internal val update = mutableListOf<(BehandlingId) -> SvarPåVilkårDto>()
    internal val delete = mutableListOf<(BehandlingId) -> SlettVilkårRequest>()

    fun opprett(builder: OpprettStønadsvilkårDsl.() -> Unit) {
        opprettScope.apply(builder)
    }

    fun oppdater(block: (behandlingId: BehandlingId) -> SvarPåVilkårDto) {
        update += block
    }

    fun slett(block: (behandlingId: BehandlingId) -> SlettVilkårRequest) {
        delete += block
    }
}

@BehandlingTestdataDslMarker
class OpprettStønadsvilkårDsl {
    private val dtoer = mutableListOf<LagreVilkår>()

    fun offentligTransport(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        dtoer += lagreDagligReiseDto(fom = fom, tom = tom)
    }

    fun add(lagreVilkår: LagreVilkår) {
        dtoer += lagreVilkår
    }

    fun add(lagreVilkår: Collection<LagreVilkår>) {
        lagreVilkår.forEach { add(it) }
    }

    fun build() = dtoer
}
