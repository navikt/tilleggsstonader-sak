package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.DekketAvAnnetRegelverkVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetsdager
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurdering
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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import java.time.LocalDate

data class LagreVilkårperiode(
    val behandlingId: BehandlingId,
    val type: VilkårperiodeType,
    val fom: LocalDate,
    val tom: LocalDate,
    val begrunnelse: String? = null,
    val kildeId: String? = null,
    val faktaOgSvar: FaktaOgSvarDto? = null,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(FaktaOgSvarMålgruppeDto::class, name = "MÅLGRUPPE"),
    JsonSubTypes.Type(FaktaOgSvarAktivitetBarnetilsynDto::class, name = "AKTIVITET_BARNETILSYN"),
    JsonSubTypes.Type(FaktaOgSvarAktivitetLæremidlerDto::class, name = "AKTIVITET_LÆREMIDLER"),
    JsonSubTypes.Type(FaktaOgSvarAktivitetBoutgifterDto::class, name = "AKTIVITET_BOUTGIFTER"),
)
sealed class FaktaOgSvarDto {
    open fun inneholderGammelManglerData(): Boolean = false
}

data class FaktaOgSvarMålgruppeDto(
    val svarMedlemskap: SvarJaNei? = null,
    val svarUtgifterDekketAvAnnetRegelverk: SvarJaNei? = null,
    val svarMottarSykepengerForFulltidsstilling: SvarJaNei? = null,
) : FaktaOgSvarDto() {
    override fun inneholderGammelManglerData(): Boolean = svarMottarSykepengerForFulltidsstilling == SvarJaNei.GAMMEL_MANGLER_DATA
}

data class FaktaOgSvarAktivitetBarnetilsynDto(
    val aktivitetsdager: Int? = null,
    val svarLønnet: SvarJaNei? = null,
) : FaktaOgSvarDto()

data class FaktaOgSvarAktivitetLæremidlerDto(
    val prosent: Int? = null,
    val studienivå: Studienivå? = null,
    val svarHarUtgifter: SvarJaNei? = null,
    val svarHarRettTilUtstyrsstipend: SvarJaNei? = null,
) : FaktaOgSvarDto()

data class FaktaOgSvarAktivitetBoutgifterDto(
    val svarLønnet: SvarJaNei? = null,
) : FaktaOgSvarDto()

fun FaktaOgVurdering.tilFaktaOgSvarDto(): FaktaOgSvarDto =
    when (this) {
        is MålgruppeFaktaOgVurdering ->
            FaktaOgSvarMålgruppeDto(
                svarMedlemskap =
                    this.vurderinger
                        .takeIfVurderinger<MedlemskapVurdering>()
                        ?.medlemskap
                        ?.svar,
                svarUtgifterDekketAvAnnetRegelverk =
                    this.vurderinger
                        .takeIfVurderinger<DekketAvAnnetRegelverkVurdering>()
                        ?.dekketAvAnnetRegelverk
                        ?.svar,
                svarMottarSykepengerForFulltidsstilling =
                    this.vurderinger
                        .takeIfVurderinger<MottarSykepengerForFulltidsstillingVurdering>()
                        ?.mottarSykepengerForFulltidsstilling
                        ?.svar,
            )

        is AktivitetTilsynBarn ->
            FaktaOgSvarAktivitetBarnetilsynDto(
                aktivitetsdager = this.fakta.takeIfFakta<FaktaAktivitetsdager>()?.aktivitetsdager,
                svarLønnet =
                    this.vurderinger
                        .takeIfVurderinger<LønnetVurdering>()
                        ?.lønnet
                        ?.svar,
            )

        is AktivitetLæremidler ->
            FaktaOgSvarAktivitetLæremidlerDto(
                svarHarRettTilUtstyrsstipend =
                    this.vurderinger
                        .takeIfVurderinger<HarRettTilUtstyrsstipendVurdering>()
                        ?.harRettTilUtstyrsstipend
                        ?.svar,
                svarHarUtgifter =
                    this.vurderinger
                        .takeIfVurderinger<HarUtgifterVurdering>()
                        ?.harUtgifter
                        ?.svar,
                prosent = this.fakta.takeIfFakta<FaktaProsent>()?.prosent,
                studienivå = this.fakta.takeIfFakta<FaktaStudienivå>()?.studienivå,
            )

        is AktivitetBoutgifter ->
            FaktaOgSvarAktivitetBoutgifterDto(
                svarLønnet =
                    this.vurderinger
                        .takeIfVurderinger<LønnetVurdering>()
                        ?.lønnet
                        ?.svar,
            )
    }
