package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AldersvilkårOppfyltFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ResultatDelvilkårperiode.IKKE_OPPFYLT
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ResultatDelvilkårperiode.OPPFYLT

sealed interface FaktaOgVurderingLæremidler : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingLæremidler
}

sealed interface MålgruppeFaktaOgVurdering : FaktaOgVurdering {
    override val fakta: AldersvilkårOppfyltFakta
}

data object AldersvilkårOppfyltFakta : Fakta {
    val aldersvilkårOppfylt: JaNei? = null
}

sealed interface MålgruppeLæremidler :
    MålgruppeFaktaOgVurdering,
    FaktaOgVurderingLæremidler {
    override val type: MålgruppeLæremidlerType
}

sealed interface AktivitetLæremidler :
    AktivitetFaktaOgVurdering,
    FaktaOgVurderingLæremidler {
    override val type: AktivitetLæremidlerType
}

data class AAPLæremidler(
    override val vurderinger: VurderingAAP,
) : MålgruppeLæremidler {
    override val type: MålgruppeLæremidlerType = MålgruppeLæremidlerType.AAP_LÆREMIDLER
    override val fakta: AldersvilkårOppfyltFakta = AldersvilkårOppfyltFakta
}

data class UføretrygdLæremidler(
    override val vurderinger: VurderingUføretrygd,
) : MålgruppeLæremidler {
    override val type: MålgruppeLæremidlerType = MålgruppeLæremidlerType.UFØRETRYGD_LÆREMIDLER
    override val fakta: AldersvilkårOppfyltFakta = AldersvilkårOppfyltFakta
}

data class NedsattArbeidsevneLæremidler(
    override val vurderinger: VurderingNedsattArbeidsevne,
) : MålgruppeLæremidler {
    override val type: MålgruppeLæremidlerType = MålgruppeLæremidlerType.NEDSATT_ARBEIDSEVNE_LÆREMIDLER
    override val fakta: AldersvilkårOppfyltFakta = AldersvilkårOppfyltFakta
}

data class OmstillingsstønadLæremidler(
    override val vurderinger: VurderingOmstillingsstønad,
) : MålgruppeLæremidler {
    override val type: MålgruppeLæremidlerType = MålgruppeLæremidlerType.OMSTILLINGSSTØNAD_LÆREMIDLER
    override val fakta: AldersvilkårOppfyltFakta = AldersvilkårOppfyltFakta
}

data object OvergangssstønadLæremidler : MålgruppeLæremidler {
    override val type: MålgruppeLæremidlerType = MålgruppeLæremidlerType.OVERGANGSSTØNAD_LÆREMIDLER
    override val vurderinger: VurderingOvergangsstønad = VurderingOvergangsstønad
    override val fakta: AldersvilkårOppfyltFakta = AldersvilkårOppfyltFakta
}

data object IngenMålgruppeLæremidler : MålgruppeLæremidler {
    override val type: MålgruppeLæremidlerType = MålgruppeLæremidlerType.INGEN_MÅLGRUPPE_LÆREMIDLER
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: AldersvilkårOppfyltFakta = AldersvilkårOppfyltFakta
}

data object SykepengerLæremidler : MålgruppeLæremidler {
    override val type: MålgruppeLæremidlerType = MålgruppeLæremidlerType.SYKEPENGER_100_PROSENT_LÆREMIDLER
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: AldersvilkårOppfyltFakta = AldersvilkårOppfyltFakta
}

data class TiltakLæremidler(
    override val fakta: FaktaAktivitetLæremidler,
    override val vurderinger: VurderingTiltakLæremidler,
) : AktivitetLæremidler {
    override val type: AktivitetLæremidlerType = AktivitetLæremidlerType.TILTAK_LÆREMIDLER

    override fun utledResultat(): ResultatVilkårperiode {
        if (vurderinger.harUtgifter.resultat == IKKE_OPPFYLT) {
            return ResultatVilkårperiode.IKKE_OPPFYLT
        }

        if (vurderinger.harUtgifter.resultat == OPPFYLT && fakta.studienivå == Studienivå.HØYERE_UTDANNING) {
            return ResultatVilkårperiode.OPPFYLT
        }

        return super.utledResultat()
    }
}

data class UtdanningLæremidler(
    override val fakta: FaktaAktivitetLæremidler,
    override val vurderinger: VurderingerUtdanningLæremidler,
) : AktivitetLæremidler {
    override val type: AktivitetLæremidlerType = AktivitetLæremidlerType.UTDANNING_LÆREMIDLER

    override fun utledResultat(): ResultatVilkårperiode {
        if (vurderinger.harUtgifter.resultat == IKKE_OPPFYLT) {
            return ResultatVilkårperiode.IKKE_OPPFYLT
        }

        if (vurderinger.harUtgifter.resultat == OPPFYLT && fakta.studienivå == Studienivå.HØYERE_UTDANNING) {
            return ResultatVilkårperiode.OPPFYLT
        }

        return super.utledResultat()
    }
}

data object IngenAktivitetLæremidler : AktivitetLæremidler {
    override val type: AktivitetLæremidlerType = AktivitetLæremidlerType.INGEN_AKTIVITET_LÆREMIDLER
    override val fakta: Fakta = IngenFakta
    override val vurderinger: Vurderinger = IngenVurderinger
}

data class VurderingTiltakLæremidler(
    override val harUtgifter: VurderingHarUtgifter,
    override val harRettTilUtstyrsstipend: VurderingHarRettTilUtstyrsstipend,
) : HarUtgifterVurdering,
    HarRettTilUtstyrsstipendVurdering

data class VurderingerUtdanningLæremidler(
    override val harUtgifter: VurderingHarUtgifter,
    override val harRettTilUtstyrsstipend: VurderingHarRettTilUtstyrsstipend,
) : HarUtgifterVurdering,
    HarRettTilUtstyrsstipendVurdering

data class FaktaAktivitetLæremidler(
    override val prosent: Int,
    override val studienivå: Studienivå?,
) : Fakta,
    FaktaProsent,
    FaktaStudienivå {
    init {
        require(prosent in 1..100) { "Prosent må være mellom 1 og 100" }
    }
}

sealed interface TypeFaktaOgVurderingLæremidler : TypeFaktaOgVurdering

enum class AktivitetLæremidlerType(
    override val vilkårperiodeType: AktivitetType,
) : TypeAktivitetOgVurdering,
    TypeFaktaOgVurderingLæremidler {
    UTDANNING_LÆREMIDLER(AktivitetType.UTDANNING),
    TILTAK_LÆREMIDLER(AktivitetType.TILTAK),
    INGEN_AKTIVITET_LÆREMIDLER(AktivitetType.INGEN_AKTIVITET),
}

enum class MålgruppeLæremidlerType(
    override val vilkårperiodeType: MålgruppeType,
) : TypeMålgruppeOgVurdering,
    TypeFaktaOgVurderingLæremidler {
    AAP_LÆREMIDLER(MålgruppeType.AAP),
    OMSTILLINGSSTØNAD_LÆREMIDLER(MålgruppeType.OMSTILLINGSSTØNAD),
    OVERGANGSSTØNAD_LÆREMIDLER(MålgruppeType.OVERGANGSSTØNAD),
    NEDSATT_ARBEIDSEVNE_LÆREMIDLER(MålgruppeType.NEDSATT_ARBEIDSEVNE),
    UFØRETRYGD_LÆREMIDLER(MålgruppeType.UFØRETRYGD),
    SYKEPENGER_100_PROSENT_LÆREMIDLER(MålgruppeType.SYKEPENGER_100_PROSENT),
    INGEN_MÅLGRUPPE_LÆREMIDLER(MålgruppeType.INGEN_MÅLGRUPPE),
}
