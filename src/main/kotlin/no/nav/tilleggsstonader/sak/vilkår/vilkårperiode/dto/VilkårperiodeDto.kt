package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.DekketAvAnnetRegelverkVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetsdager
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfVurderinger
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaProsent
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaStudienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.HarRettTilUtstyrsstipendVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.HarUtgifterVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.LønnetVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MedlemskapVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.VilkårperiodeTypeDeserializer
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class VilkårperiodeDto(
    val id: UUID,
    @JsonDeserialize(using = VilkårperiodeTypeDeserializer::class)
    val type: VilkårperiodeType,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val delvilkår: DelvilkårVilkårperiodeDto,
    val resultat: ResultatVilkårperiode,
    val begrunnelse: String?,
    val kilde: KildeVilkårsperiode,
    val slettetKommentar: String?,
    val sistEndret: LocalDateTime,
    val aktivitetsdager: Int? = null,
    val forrigeVilkårperiodeId: UUID?,
    val status: Vilkårstatus?,
    val kildeId: String?,
    val faktaOgVurderinger: FaktaOgVurderingerDto,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}

fun Vilkårperiode.tilDto() =
    VilkårperiodeDto(
        id = this.id,
        type = this.type,
        fom = this.fom,
        tom = this.tom,
        delvilkår = faktaOgVurdering.tilDelvilkårDto(),
        resultat = this.resultat,
        begrunnelse = this.begrunnelse,
        kilde = this.kilde,
        aktivitetsdager = this.faktaOgVurdering.fakta.takeIfFakta<FaktaAktivitetsdager>()
            ?.aktivitetsdager,
        slettetKommentar = this.slettetKommentar,
        sistEndret = this.sporbar.endret.endretTid,
        forrigeVilkårperiodeId = this.forrigeVilkårperiodeId,
        status = this.status,
        kildeId = this.kildeId,
        faktaOgVurderinger = this.faktaOgVurdering.tilFaktaOgVurderingDto(),
    )

fun FaktaOgVurdering.tilDelvilkårDto(): DelvilkårVilkårperiodeDto {
    return when (this) {
        is MålgruppeFaktaOgVurdering -> DelvilkårMålgruppeDto(
            medlemskap = vurderinger.takeIfVurderinger<MedlemskapVurdering>()?.medlemskap?.tilDto(),
            dekketAvAnnetRegelverk = vurderinger.takeIfVurderinger<DekketAvAnnetRegelverkVurdering>()?.dekketAvAnnetRegelverk?.tilDto(),
        )

        is AktivitetFaktaOgVurdering -> DelvilkårAktivitetDto(
            lønnet = vurderinger.takeIfVurderinger<LønnetVurdering>()?.lønnet?.tilDto(),
        )
    }
}

// Returnerer ikke vurdering hvis resultatet er IKKE_AKTUELT
fun Vurdering.tilDto() =
    this.takeIf { resultat != ResultatDelvilkårperiode.IKKE_AKTUELT }
        ?.let {
            VurderingDto(svar = svar, resultat = resultat)
        }

data class Datoperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate>, Mergeable<LocalDate, Datoperiode> {
    override fun merge(other: Datoperiode): Datoperiode {
        return this.copy(fom = minOf(this.fom, other.fom), tom = maxOf(this.tom, other.tom))
    }
}

fun Periode<LocalDate>.formattertPeriodeNorskFormat() = "${this.fom.norskFormat()} - ${this.tom.norskFormat()}"

/**
 *  @return En sortert map kategorisert på periodetype med de oppfylte vilkårsperiodene. Periodene slåes sammen dersom
 *  de er sammenhengende, også selv om de har overlapp.
 */
fun List<VilkårperiodeDto>.mergeSammenhengendeOppfylteVilkårperioder(): Map<VilkårperiodeType, List<Datoperiode>> {
    return this.sorted().filter { it.resultat == ResultatVilkårperiode.OPPFYLT }.groupBy { it.type }
        .mapValues {
            it.value.map { Datoperiode(it.fom, it.tom) }
                .mergeSammenhengende { a, b -> a.overlapper(b) || a.tom.plusDays(1) == b.fom }
        }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(MålgruppeFaktaOgVurderingerDto::class, name = "MÅLGRUPPE"),
    JsonSubTypes.Type(AktivitetBarnetilsynFaktaOgVurderingerDto::class, name = "AKTIVITET_BARNETILSYN"),
    JsonSubTypes.Type(AktivitetLæremidlerFaktaOgVurderingerDto::class, name = "AKTIVITET_LÆREMIDLER"),
)
sealed class FaktaOgVurderingerDto

data class MålgruppeFaktaOgVurderingerDto(
    val medlemskap: VurderingDto? = null,
    val utgifterDekketAvAnnetRegelverk: VurderingDto? = null,
) : FaktaOgVurderingerDto()

data class AktivitetBarnetilsynFaktaOgVurderingerDto(
    val aktivitetsdager: Int? = null,
    val lønnet: VurderingDto? = null,
) : FaktaOgVurderingerDto()

data class AktivitetLæremidlerFaktaOgVurderingerDto(
    val prosent: Int? = null,
    val studienivå: Studienivå? = null,
    val harUtgifter: VurderingDto? = null,
    val harRettTilStudiestipend: VurderingDto? = null,
) : FaktaOgVurderingerDto()

fun FaktaOgVurdering.tilFaktaOgVurderingDto(): FaktaOgVurderingerDto {
    return when (this) {
        is MålgruppeFaktaOgVurdering -> MålgruppeFaktaOgVurderingerDto(
            medlemskap = vurderinger.takeIfVurderinger<MedlemskapVurdering>()?.medlemskap?.tilDto(),
            utgifterDekketAvAnnetRegelverk = vurderinger.takeIfVurderinger<DekketAvAnnetRegelverkVurdering>()?.dekketAvAnnetRegelverk?.tilDto(),
        )

        is AktivitetFaktaOgVurdering -> {
            when (this) {
                is FaktaOgVurderingTilsynBarn -> AktivitetBarnetilsynFaktaOgVurderingerDto(
                    aktivitetsdager = fakta.takeIfFakta<FaktaAktivitetsdager>()?.aktivitetsdager,
                    lønnet = vurderinger.takeIfVurderinger<LønnetVurdering>()?.lønnet?.tilDto(),
                )

                is FaktaOgVurderingLæremidler -> AktivitetLæremidlerFaktaOgVurderingerDto(
                    prosent = fakta.takeIfFakta<FaktaProsent>()?.prosent,
                    studienivå = fakta.takeIfFakta<FaktaStudienivå>()?.studienivå,
                    harUtgifter = vurderinger.takeIfVurderinger<HarUtgifterVurdering>()?.harUtgifter?.tilDto(),
                    harRettTilStudiestipend = vurderinger.takeIfVurderinger<HarRettTilUtstyrsstipendVurdering>()?.harRettTilUtstyrsstipend?.tilDto(),
                )
            }
        }
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(DelvilkårMålgruppeDto::class, name = "MÅLGRUPPE"),
    JsonSubTypes.Type(DelvilkårAktivitetDto::class, name = "AKTIVITET"),
)
sealed class DelvilkårVilkårperiodeDto

data class DelvilkårMålgruppeDto(
    val medlemskap: VurderingDto?,
    val dekketAvAnnetRegelverk: VurderingDto?,
) : DelvilkårVilkårperiodeDto()

data class DelvilkårAktivitetDto(
    val lønnet: VurderingDto?,
) : DelvilkårVilkårperiodeDto()

data class VurderingDto(
    val svar: SvarJaNei? = null,
    val resultat: ResultatDelvilkårperiode? = null,
)

data class SlettVikårperiode(
    val behandlingId: BehandlingId,
    val kommentar: String? = null,
)

data class VilkårperioderDto(
    val målgrupper: List<VilkårperiodeDto>,
    val aktiviteter: List<VilkårperiodeDto>,
)

data class VilkårperioderResponse(
    val vilkårperioder: VilkårperioderDto,
    val grunnlag: VilkårperioderGrunnlagDto?,
)

fun Vilkårperioder.tilDto() = VilkårperioderDto(
    målgrupper = målgrupper.map(Vilkårperiode::tilDto),
    aktiviteter = aktiviteter.map(Vilkårperiode::tilDto),
)
