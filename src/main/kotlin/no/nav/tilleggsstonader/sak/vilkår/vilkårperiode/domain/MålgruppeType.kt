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
        faktiskMålgruppe = null,
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
        faktiskMålgruppe = null,
    ),
    INGEN_MÅLGRUPPE(
        gyldigeAktiviter = emptySet(),
        faktiskMålgruppe = null,
    ),
    KVALIFISERINGSSTØNAD(
        gyldigeAktiviter = setOf(AktivitetType.TILTAK),
        faktiskMålgruppe = null,
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
            AAP, UFØRETRYGD, NEDSATT_ARBEIDSEVNE, OMSTILLINGSSTØNAD -> true
            OVERGANGSSTØNAD, INGEN_MÅLGRUPPE, SYKEPENGER_100_PROSENT, DAGPENGER, TILTAKSPENGER, KVALIFISERINGSSTØNAD -> false
        }

    fun kanBrukesForStønad(stønadstype: Stønadstype): Boolean =
        when (stønadstype) {
            Stønadstype.BARNETILSYN, Stønadstype.LÆREMIDLER, Stønadstype.BOUTGIFTER ->
                when (this) {
                    AAP -> true
                    DAGPENGER -> false
                    NEDSATT_ARBEIDSEVNE -> true
                    OMSTILLINGSSTØNAD -> true
                    OVERGANGSSTØNAD -> true
                    UFØRETRYGD -> true
                    SYKEPENGER_100_PROSENT -> true
                    INGEN_MÅLGRUPPE -> true
                    TILTAKSPENGER -> false
                    KVALIFISERINGSSTØNAD -> false
                }

            Stønadstype.DAGLIG_REISE_TSO ->
                when (this) {
                    AAP -> true
                    DAGPENGER -> false
                    NEDSATT_ARBEIDSEVNE -> true
                    OMSTILLINGSSTØNAD -> true
                    OVERGANGSSTØNAD -> true
                    UFØRETRYGD -> true
                    SYKEPENGER_100_PROSENT -> false
                    INGEN_MÅLGRUPPE -> true
                    TILTAKSPENGER -> false
                    KVALIFISERINGSSTØNAD -> false
                }

            Stønadstype.DAGLIG_REISE_TSR ->
                when (this) {
                    INGEN_MÅLGRUPPE -> true
                    TILTAKSPENGER -> true
                    KVALIFISERINGSSTØNAD -> true
                    AAP -> false
                    DAGPENGER -> true
                    NEDSATT_ARBEIDSEVNE -> false
                    OMSTILLINGSSTØNAD -> false
                    OVERGANGSSTØNAD -> false
                    UFØRETRYGD -> false
                    SYKEPENGER_100_PROSENT -> false
                }
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
