package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AldersvilkårVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.DekketAvAnnetRegelverkVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetsdager
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingBoutgifter
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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MottarSykepengerForFulltidsstillingVurdering
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
    val resultat: ResultatVilkårperiode,
    val begrunnelse: String?,
    val kilde: KildeVilkårsperiode,
    val slettetKommentar: String?,
    val sistEndret: LocalDateTime,
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
        resultat = this.resultat,
        begrunnelse = this.begrunnelse,
        kilde = this.kilde,
        slettetKommentar = this.slettetKommentar,
        sistEndret = this.sporbar.endret.endretTid,
        forrigeVilkårperiodeId = this.forrigeVilkårperiodeId,
        status = this.status,
        kildeId = this.kildeId,
        faktaOgVurderinger = this.faktaOgVurdering.tilFaktaOgVurderingDto(),
    )

// Returnerer ikke vurdering hvis resultatet er IKKE_AKTUELT
fun Vurdering.tilDto() =
    this
        .takeIf { resultat != ResultatDelvilkårperiode.IKKE_AKTUELT }
        ?.let {
            VurderingDto(svar = svar, resultat = resultat)
        }

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(MålgruppeFaktaOgVurderingerDto::class, name = "MÅLGRUPPE"),
    JsonSubTypes.Type(MålgruppeLæremidlerFaktaOgVurderingerDto::class, name = "MÅLGRUPPE_LÆREMIDLER"),
    JsonSubTypes.Type(AktivitetBarnetilsynFaktaOgVurderingerDto::class, name = "AKTIVITET_BARNETILSYN"),
    JsonSubTypes.Type(AktivitetLæremidlerFaktaOgVurderingerDto::class, name = "AKTIVITET_LÆREMIDLER"),
    JsonSubTypes.Type(AktivitetBoutgifterFaktaOgVurderingerDto::class, name = "AKTIVITET_BOUTGIFTER"),
)
sealed class FaktaOgVurderingerDto

data class MålgruppeFaktaOgVurderingerDto(
    val medlemskap: VurderingDto? = null,
    val utgifterDekketAvAnnetRegelverk: VurderingDto? = null,
    val aldersvilkår: VurderingDto? = null,
    val mottarSykepengerForFulltidsstilling: VurderingDto? = null,
    ) : FaktaOgVurderingerDto()

data class MålgruppeLæremidlerFaktaOgVurderingerDto(
    val medlemskap: VurderingDto? = null,
    val utgifterDekketAvAnnetRegelverk: VurderingDto? = null,
    val aldersvilkår: VurderingDto? = null,
) : FaktaOgVurderingerDto()

data class AktivitetBarnetilsynFaktaOgVurderingerDto(
    val aktivitetsdager: Int? = null,
    val lønnet: VurderingDto? = null,
) : FaktaOgVurderingerDto()

data class AktivitetLæremidlerFaktaOgVurderingerDto(
    val prosent: Int? = null,
    val studienivå: Studienivå? = null,
    val harUtgifter: VurderingDto? = null,
    val harRettTilUtstyrsstipend: VurderingDto? = null,
) : FaktaOgVurderingerDto()

data class AktivitetBoutgifterFaktaOgVurderingerDto(
    val lønnet: VurderingDto? = null,
) : FaktaOgVurderingerDto()

fun FaktaOgVurdering.tilFaktaOgVurderingDto(): FaktaOgVurderingerDto =
    when (this) {
        is MålgruppeFaktaOgVurdering ->
            when (this) {
                is FaktaOgVurderingLæremidler ->
                    MålgruppeLæremidlerFaktaOgVurderingerDto(
                        medlemskap = vurderinger.takeIfVurderinger<MedlemskapVurdering>()?.medlemskap?.tilDto(),
                        utgifterDekketAvAnnetRegelverk =
                            vurderinger.takeIfVurderinger<DekketAvAnnetRegelverkVurdering>()?.dekketAvAnnetRegelverk?.tilDto(),
                        aldersvilkår =
                            vurderinger
                                .takeIfVurderinger<AldersvilkårVurdering>()
                                ?.aldersvilkår
                                ?.takeIf { it.svar != SvarJaNei.GAMMEL_MANGLER_DATA }
                                ?.tilDto(),                    )

                else ->
                    MålgruppeFaktaOgVurderingerDto(
                        medlemskap = vurderinger.takeIfVurderinger<MedlemskapVurdering>()?.medlemskap?.tilDto(),
                        utgifterDekketAvAnnetRegelverk =
                            vurderinger.takeIfVurderinger<DekketAvAnnetRegelverkVurdering>()?.dekketAvAnnetRegelverk?.tilDto(),
                        aldersvilkår =
                            vurderinger
                                .takeIfVurderinger<AldersvilkårVurdering>()
                                ?.aldersvilkår
                                ?.takeIf { it.svar != SvarJaNei.GAMMEL_MANGLER_DATA }
                                ?.tilDto(),
                        // TODO: Håndter gammel mangler data
                        mottarSykepengerForFulltidsstilling =
                            vurderinger
                                .takeIfVurderinger<MottarSykepengerForFulltidsstillingVurdering>()
                                ?.mottarSykepengerForFulltidsstilling
                                ?.tilDto(),
                        )
            }

        is AktivitetFaktaOgVurdering -> {
            when (this) {
                is FaktaOgVurderingTilsynBarn ->
                    AktivitetBarnetilsynFaktaOgVurderingerDto(
                        aktivitetsdager = fakta.takeIfFakta<FaktaAktivitetsdager>()?.aktivitetsdager,
                        lønnet = vurderinger.takeIfVurderinger<LønnetVurdering>()?.lønnet?.tilDto(),
                    )

                is FaktaOgVurderingLæremidler ->
                    AktivitetLæremidlerFaktaOgVurderingerDto(
                        prosent = fakta.takeIfFakta<FaktaProsent>()?.prosent,
                        studienivå = fakta.takeIfFakta<FaktaStudienivå>()?.studienivå,
                        harUtgifter = vurderinger.takeIfVurderinger<HarUtgifterVurdering>()?.harUtgifter?.tilDto(),
                        harRettTilUtstyrsstipend =
                            vurderinger
                                .takeIfVurderinger<HarRettTilUtstyrsstipendVurdering>()
                                ?.harRettTilUtstyrsstipend
                                ?.tilDto(),
                    )

                is FaktaOgVurderingBoutgifter ->
                    AktivitetBoutgifterFaktaOgVurderingerDto(
                        lønnet = vurderinger.takeIfVurderinger<LønnetVurdering>()?.lønnet?.tilDto(),
                    )
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

fun Vilkårperioder.tilDto() =
    VilkårperioderDto(
        målgrupper = målgrupper.map(Vilkårperiode::tilDto),
        aktiviteter = aktiviteter.map(Vilkårperiode::tilDto),
    )
