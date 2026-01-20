package no.nav.tilleggsstonader.sak.integrasjonstest.dsl

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårLøpendeUtgifterEnBolig
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.oppfylteDelvilkårPassBarnDto
import java.time.LocalDate
import java.time.YearMonth

@BehandlingTestdataDslMarker
class StønadsvilkårTestdataDsl {
    internal val opprettScope = OpprettStønadsvilkårDsl()
    internal val update = mutableListOf<(VilkårsvurderingDto) -> SvarPåVilkårDto>()
    internal val updateDagligReise = mutableListOf<(List<VilkårDagligReiseDto>) -> Pair<VilkårId, LagreDagligReiseDto>>()
    internal val delete = mutableListOf<(VilkårsvurderingDto) -> SlettVilkårRequest>()
    internal val deleteDagligReise = mutableListOf<(List<VilkårDagligReiseDto>) -> Pair<VilkårId, SlettVilkårRequestDto>>()

    fun opprett(builder: OpprettStønadsvilkårDsl.() -> Unit) {
        opprettScope.apply(builder)
    }

    fun oppdater(block: (vilkårsvurderingDto: VilkårsvurderingDto) -> SvarPåVilkårDto) {
        update += block
    }

    fun oppdaterDagligReise(block: (vilkårDagligReise: List<VilkårDagligReiseDto>) -> Pair<VilkårId, LagreDagligReiseDto>) {
        updateDagligReise += block
    }

    fun slett(block: (vilkårsvurderingDto: VilkårsvurderingDto) -> SlettVilkårRequest) {
        delete += block
    }

    fun slettDagligReise(block: (vilkårDagligReise: List<VilkårDagligReiseDto>) -> Pair<VilkårId, SlettVilkårRequestDto>) {
        deleteDagligReise += block
    }
}

@BehandlingTestdataDslMarker
class OpprettStønadsvilkårDsl {
    private val dtoer = mutableListOf<(BehandlingId, Collection<BarnId>) -> LagreVilkår>()

    fun offentligTransport(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        dtoer += { _, _ ->
            lagreDagligReiseDto(fom = fom, tom = tom)
        }
    }

    fun løpendeutgifterEnBolig(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        add { behandlingId, _ ->
            OpprettVilkårDto(
                fom = fom,
                tom = tom,
                behandlingId = behandlingId,
                vilkårType = VilkårType.LØPENDE_UTGIFTER_EN_BOLIG,
                delvilkårsett = oppfylteDelvilkårLøpendeUtgifterEnBolig().map { it.tilDto() },
                barnId = null,
                utgift = 100,
                erFremtidigUtgift = false,
                offentligTransport = null,
            )
        }
    }

    fun passBarn(
        fom: YearMonth,
        tom: YearMonth,
        utgift: Int,
    ) {
        add { behandlingId, barnIder ->
            OpprettVilkårDto(
                vilkårType = VilkårType.PASS_BARN,
                barnId = barnIder.first(),
                behandlingId = behandlingId,
                delvilkårsett = oppfylteDelvilkårPassBarnDto(),
                fom = fom.atDay(1),
                tom = tom.atEndOfMonth(),
                utgift = utgift,
                erFremtidigUtgift = false,
                offentligTransport = null,
            )
        }
    }

    fun add(block: (BehandlingId, Collection<BarnId>) -> LagreVilkår) {
        dtoer += block
    }

    fun add(lagreVilkår: Collection<(BehandlingId, Collection<BarnId>) -> LagreVilkår>) {
        lagreVilkår.forEach { add(it) }
    }

    fun build(
        behandlingId: BehandlingId,
        barnIder: Collection<BarnId>,
    ) = dtoer.map { it(behandlingId, barnIder) }
}
