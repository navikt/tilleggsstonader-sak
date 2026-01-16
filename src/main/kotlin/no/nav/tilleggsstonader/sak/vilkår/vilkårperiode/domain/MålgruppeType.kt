package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe

/**
 * @param prioritet lavest er den som har høyest prioritet
 */
enum class MålgruppeType(
    val gyldigeAktiviter: Set<AktivitetType>,
    private val faktiskMålgruppe: FaktiskMålgruppe?,
) : VilkårperiodeType {
    AAP(
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
        faktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
    ),
    DAGPENGER(
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
        faktiskMålgruppe = FaktiskMålgruppe.ARBEIDSSØKER,
    ),
    OMSTILLINGSSTØNAD(
        gyldigeAktiviter = setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING),
        faktiskMålgruppe = FaktiskMålgruppe.GJENLEVENDE,
    ),
    OVERGANGSSTØNAD(
        gyldigeAktiviter = setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING),
        faktiskMålgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER,
    ),
    NEDSATT_ARBEIDSEVNE(
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
        faktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
    ),
    UFØRETRYGD(
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
        faktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
    ),
    SYKEPENGER_100_PROSENT(
        gyldigeAktiviter = emptySet(),
        faktiskMålgruppe = null,
    ),
    TILTAKSPENGER(
        gyldigeAktiviter = setOf(AktivitetType.TILTAK),
        faktiskMålgruppe = FaktiskMålgruppe.ARBEIDSSØKER,
    ),
    INGEN_MÅLGRUPPE(
        gyldigeAktiviter = emptySet(),
        faktiskMålgruppe = null,
    ),
    KVALIFISERINGSSTØNAD(
        gyldigeAktiviter = setOf(AktivitetType.TILTAK),
        faktiskMålgruppe = FaktiskMålgruppe.ARBEIDSSØKER,
    ),
    INNSATT_I_FENGSEL(
        gyldigeAktiviter = setOf(AktivitetType.TILTAK),
        faktiskMålgruppe = FaktiskMålgruppe.ARBEIDSSØKER,
    ),
    ;

    override fun tilDbType(): String = this.name

    fun gjelderNedsattArbeidsevne() = this == NEDSATT_ARBEIDSEVNE || this == UFØRETRYGD || this == AAP

    fun faktiskMålgruppe() = this.faktiskMålgruppe ?: error("Mangler faktisk målgruppe for $this")

    override fun girIkkeRettPåVedtaksperiode() =
        this == INGEN_MÅLGRUPPE ||
            this == SYKEPENGER_100_PROSENT

    fun skalVurdereAldersvilkår() =
        when (this) {
            AAP,
            UFØRETRYGD,
            NEDSATT_ARBEIDSEVNE,
            OMSTILLINGSSTØNAD,
            -> true

            OVERGANGSSTØNAD,
            INGEN_MÅLGRUPPE,
            SYKEPENGER_100_PROSENT,
            DAGPENGER,
            TILTAKSPENGER,
            KVALIFISERINGSSTØNAD,
            INNSATT_I_FENGSEL,
            -> false
        }

    fun kanBrukesForStønad(stønadstype: Stønadstype): Boolean =
        when (stønadstype) {
            Stønadstype.BARNETILSYN, Stønadstype.LÆREMIDLER, Stønadstype.BOUTGIFTER ->
                this in
                    listOf(
                        AAP,
                        NEDSATT_ARBEIDSEVNE,
                        OMSTILLINGSSTØNAD,
                        OVERGANGSSTØNAD,
                        UFØRETRYGD,
                        SYKEPENGER_100_PROSENT,
                        INGEN_MÅLGRUPPE,
                    )

            Stønadstype.DAGLIG_REISE_TSO ->
                this in
                    listOf(
                        AAP,
                        NEDSATT_ARBEIDSEVNE,
                        OMSTILLINGSSTØNAD,
                        OVERGANGSSTØNAD,
                        UFØRETRYGD,
                        INGEN_MÅLGRUPPE,
                    )

            Stønadstype.DAGLIG_REISE_TSR ->
                this in
                    listOf(
                        INGEN_MÅLGRUPPE,
                        TILTAKSPENGER,
                        KVALIFISERINGSSTØNAD,
                        DAGPENGER,
                        INNSATT_I_FENGSEL,
                    )
        }
}

fun Hovedytelse.tilMålgruppeType(): MålgruppeType =
    when (this) {
        Hovedytelse.AAP -> MålgruppeType.AAP
        Hovedytelse.OVERGANGSSTØNAD -> MålgruppeType.OVERGANGSSTØNAD
        Hovedytelse.GJENLEVENDEPENSJON -> MålgruppeType.OMSTILLINGSSTØNAD
        Hovedytelse.UFØRETRYGD -> MålgruppeType.UFØRETRYGD
        Hovedytelse.TILTAKSPENGER -> MålgruppeType.TILTAKSPENGER
        Hovedytelse.DAGPENGER -> MålgruppeType.DAGPENGER
        Hovedytelse.SYKEPENGER -> MålgruppeType.SYKEPENGER_100_PROSENT
        Hovedytelse.KVALIFISERINGSSTØNAD -> MålgruppeType.KVALIFISERINGSSTØNAD
        Hovedytelse.INGEN_PENGESTØTTE -> MålgruppeType.INGEN_MÅLGRUPPE
        Hovedytelse.INGEN_PASSENDE_ALTERNATIVER -> MålgruppeType.INGEN_MÅLGRUPPE
    }
