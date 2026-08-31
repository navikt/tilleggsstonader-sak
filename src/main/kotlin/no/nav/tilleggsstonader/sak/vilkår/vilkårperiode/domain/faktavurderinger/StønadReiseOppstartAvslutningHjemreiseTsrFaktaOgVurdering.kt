package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

sealed interface FaktaOgVurderingReiseOppstartAvslutningHjemreiseTsr : FaktaOgVurdering {
    override val type: TypeFaktaOgVurderingReiseOppstartAvslutningHjemreiseTsr
}

sealed interface MålgruppeReiseOppstartAvslutningHjemreiseTsr :
    MålgruppeFaktaOgVurdering,
    FaktaOgVurderingReiseOppstartAvslutningHjemreiseTsr {
    override val type: MålgruppeReiseOppstartAvslutningHjemreiseTsrType
}

sealed interface AktivitetReiseOppstartAvslutningHjemreiseTsr :
    AktivitetFaktaOgVurdering,
    FaktaOgVurderingReiseOppstartAvslutningHjemreiseTsr {
    override val type: AktivitetReiseOppstartAvslutningHjemreiseTsrType
}

data class TiltakReiseOppstartAvslutningHjemreiseTsr(
    override val vurderinger: VurderingTiltakReiseOppstartAvslutningHjemreiseTsr,
) : AktivitetReiseOppstartAvslutningHjemreiseTsr {
    override val type: AktivitetReiseOppstartAvslutningHjemreiseTsrType =
        AktivitetReiseOppstartAvslutningHjemreiseTsrType.TILTAK_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR
    override val fakta: IngenFakta = IngenFakta
}

data object IngenAktivitetReiseOppstartAvslutningHjemreiseTsr : AktivitetReiseOppstartAvslutningHjemreiseTsr {
    override val type: AktivitetReiseOppstartAvslutningHjemreiseTsrType =
        AktivitetReiseOppstartAvslutningHjemreiseTsrType.INGEN_AKTIVITET_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR
    override val fakta: IngenFakta = IngenFakta
    override val vurderinger: Vurderinger = IngenVurderinger
}

data object IngenMålgruppeReiseOppstartAvslutningHjemreiseTsr : MålgruppeReiseOppstartAvslutningHjemreiseTsr {
    override val type: MålgruppeReiseOppstartAvslutningHjemreiseTsrType =
        MålgruppeReiseOppstartAvslutningHjemreiseTsrType.INGEN_MÅLGRUPPE_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR
    override val vurderinger: IngenVurderinger = IngenVurderinger
    override val fakta: IngenFakta = IngenFakta
}

data class DagpengerReiseOppstartAvslutningHjemreiseTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeReiseOppstartAvslutningHjemreiseTsr {
    override val type: MålgruppeReiseOppstartAvslutningHjemreiseTsrType =
        MålgruppeReiseOppstartAvslutningHjemreiseTsrType.DAGPENGER_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR
    override val fakta: IngenFakta = IngenFakta
}

data class TiltakspengerReiseOppstartAvslutningHjemreiseTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeReiseOppstartAvslutningHjemreiseTsr {
    override val type: MålgruppeReiseOppstartAvslutningHjemreiseTsrType =
        MålgruppeReiseOppstartAvslutningHjemreiseTsrType.TILTAKSPENGER_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR
    override val fakta: IngenFakta = IngenFakta
}

data class KvalifiseringsstønadReiseOppstartAvslutningHjemreiseTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeReiseOppstartAvslutningHjemreiseTsr {
    override val type: MålgruppeReiseOppstartAvslutningHjemreiseTsrType =
        MålgruppeReiseOppstartAvslutningHjemreiseTsrType.KVALIFISERINGSSTØNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR
    override val fakta: IngenFakta = IngenFakta
}

data class InnsattIFengselReiseOppstartAvslutningHjemreiseTsr(
    override val vurderinger: IngenVurderinger = IngenVurderinger,
) : MålgruppeReiseOppstartAvslutningHjemreiseTsr {
    override val type: MålgruppeReiseOppstartAvslutningHjemreiseTsrType =
        MålgruppeReiseOppstartAvslutningHjemreiseTsrType.INNSATT_I_FENGSEL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR
    override val fakta: IngenFakta = IngenFakta
}

data class VurderingTiltakReiseOppstartAvslutningHjemreiseTsr(
    override val lønnet: VurderingLønnet,
    override val harUtgifter: VurderingHarUtgifter,
    override val erAktivitetenObligatorisk: VurderingErAktivitetenObligatorisk,
) : HarUtgifterVurdering,
    LønnetVurdering,
    ErAktivitetenObligatoriskVurdering

sealed interface TypeFaktaOgVurderingReiseOppstartAvslutningHjemreiseTsr : TypeFaktaOgVurdering

enum class AktivitetReiseOppstartAvslutningHjemreiseTsrType(
    override val vilkårperiodeType: AktivitetType,
) : TypeAktivitetOgVurdering,
    TypeFaktaOgVurderingReiseOppstartAvslutningHjemreiseTsr {
    TILTAK_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR(AktivitetType.TILTAK),
    INGEN_AKTIVITET_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR(AktivitetType.INGEN_AKTIVITET),
}

enum class MålgruppeReiseOppstartAvslutningHjemreiseTsrType(
    override val vilkårperiodeType: MålgruppeType,
) : TypeMålgruppeOgVurdering,
    TypeFaktaOgVurderingReiseOppstartAvslutningHjemreiseTsr {
    INGEN_MÅLGRUPPE_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR(MålgruppeType.INGEN_MÅLGRUPPE),
    DAGPENGER_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR(MålgruppeType.DAGPENGER),
    TILTAKSPENGER_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR(MålgruppeType.TILTAKSPENGER),
    KVALIFISERINGSSTØNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR(MålgruppeType.KVALIFISERINGSSTØNAD),
    INNSATT_I_FENGSEL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR(MålgruppeType.INNSATT_I_FENGSEL),
}
