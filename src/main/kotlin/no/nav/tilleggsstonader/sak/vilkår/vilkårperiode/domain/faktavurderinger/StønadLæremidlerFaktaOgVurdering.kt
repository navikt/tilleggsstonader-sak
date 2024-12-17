package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

sealed interface FaktaOgVurderingLæremidler : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingLæremidler
}

sealed interface MålgruppeLæremidler : MålgruppeFaktaOgVurdering, FaktaOgVurderingLæremidler {
    override val type: MålgruppeLæremidlerType
}

sealed interface AktivitetLæremidler : AktivitetFaktaOgVurdering, FaktaOgVurderingLæremidler {
    override val type: AktivitetLæremidlerType
}

data class AAPLæremidler(
    override val vurderinger: VurderingAAP,
) : MålgruppeLæremidler {
    override val type: MålgruppeLæremidlerType = MålgruppeLæremidlerType.AAP_LÆREMIDLER
    override val fakta: IngenFakta = IngenFakta
}

data class UføretrygdLæremidler(
    override val vurderinger: VurderingUføretrygd,
) : MålgruppeLæremidler {
    override val type: MålgruppeLæremidlerType = MålgruppeLæremidlerType.UFØRETRYGD_LÆREMIDLER
    override val fakta: IngenFakta = IngenFakta
}

data class NedsattArbeidsevneLæremidler(
    override val vurderinger: VurderingNedsattArbeidsevne,
) : MålgruppeLæremidler {
    override val type: MålgruppeLæremidlerType = MålgruppeLæremidlerType.NEDSATT_ARBEIDSEVNE_LÆREMIDLER
    override val fakta: IngenFakta = IngenFakta
}

data class OmstillingsstønadLæremidler(
    override val vurderinger: VurderingOmstillingsstønad,
) : MålgruppeLæremidler {
    override val type: MålgruppeLæremidlerType = MålgruppeLæremidlerType.OMSTILLINGSSTØNAD_LÆREMIDLER
    override val fakta: IngenFakta = IngenFakta
}

data object OvergangssstønadLæremidler : MålgruppeLæremidler {
    override val type: MålgruppeLæremidlerType = MålgruppeLæremidlerType.OVERGANGSSTØNAD_LÆREMIDLER
    override val vurderinger: VurderingOvergangsstønad = VurderingOvergangsstønad
    override val fakta: IngenFakta = IngenFakta
}

data object IngenMålgruppeLæremidler : MålgruppeLæremidler {
    override val type: MålgruppeLæremidlerType = MålgruppeLæremidlerType.INGEN_MÅLGRUPPE_LÆREMIDLER
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

data object SykepengerLæremidler : MålgruppeLæremidler {
    override val type: MålgruppeLæremidlerType = MålgruppeLæremidlerType.SYKEPENGER_100_PROSENT_LÆREMIDLER
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

data class TiltakLæremidler(
    override val fakta: FaktaAktivitetLæremidler,
    override val vurderinger: VurderingTiltakLæremidler,
) : AktivitetLæremidler {
    override val type: AktivitetLæremidlerType = AktivitetLæremidlerType.TILTAK_LÆREMIDLER
}

data class UtdanningLæremidler(
    override val fakta: FaktaAktivitetLæremidler,
    override val vurderinger: VurderingerUtdanningLæremidler,
) : AktivitetLæremidler {
    override val type: AktivitetLæremidlerType = AktivitetLæremidlerType.UTDANNING_LÆREMIDLER
}

data object IngenUtdanningLæremidler : AktivitetLæremidler {
    override val type: AktivitetLæremidlerType = AktivitetLæremidlerType.INGEN_UTDANNING_LÆREMIDLER
    override val fakta: Fakta = IngenFakta
    override val vurderinger: Vurderinger = IngenVurderinger
}

data class VurderingTiltakLæremidler(
    override val harUtgifter: VurderingHarUtgifter,
    override val harRettTilUtstyrsstipend: VurderingHarRettTilUtstyrsstipend,
) : HarUtgifterVurdering, HarRettTilUtstyrsstipendVurdering

data class VurderingerUtdanningLæremidler(
    override val harRettTilUtstyrsstipend: VurderingHarRettTilUtstyrsstipend,
) : HarRettTilUtstyrsstipendVurdering

data class FaktaAktivitetLæremidler(
    override val prosent: Int,
    override val studienivå: Studienivå,
) : Fakta, FaktaProsent, FaktaStudienivå {
    init {
        require(prosent in 1..100) { "Prosent må være mellom 1 og 100" }
    }
}

sealed interface TypeFaktaOgVurderingLæremidler : TypeFaktaOgVurdering

enum class AktivitetLæremidlerType(
    override val vilkårperiodeType: AktivitetType,
) : TypeAktivitetOgVurdering, TypeFaktaOgVurderingLæremidler {

    UTDANNING_LÆREMIDLER(AktivitetType.UTDANNING),
    TILTAK_LÆREMIDLER(AktivitetType.TILTAK),
    INGEN_UTDANNING_LÆREMIDLER(AktivitetType.INGEN_UTDANNING),
}

enum class MålgruppeLæremidlerType(
    override val vilkårperiodeType: MålgruppeType,
) : TypeMålgruppeOgVurdering, TypeFaktaOgVurderingLæremidler {

    AAP_LÆREMIDLER(MålgruppeType.AAP),
    OMSTILLINGSSTØNAD_LÆREMIDLER(MålgruppeType.OMSTILLINGSSTØNAD),
    OVERGANGSSTØNAD_LÆREMIDLER(MålgruppeType.OVERGANGSSTØNAD),
    NEDSATT_ARBEIDSEVNE_LÆREMIDLER(MålgruppeType.NEDSATT_ARBEIDSEVNE),
    UFØRETRYGD_LÆREMIDLER(MålgruppeType.UFØRETRYGD),
    SYKEPENGER_100_PROSENT_LÆREMIDLER(MålgruppeType.SYKEPENGER_100_PROSENT),
    INGEN_MÅLGRUPPE_LÆREMIDLER(MålgruppeType.INGEN_MÅLGRUPPE),
}
