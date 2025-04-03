package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel

/**
 * @param prioritet lavest er den som har høyest prioritet
 */
enum class MålgruppeType(
    private val prioritet: Int?,
    val gyldigeAktiviter: Set<AktivitetType>,
    private val faktiskMålgruppe: FaktiskMålgruppe?,
) : VilkårperiodeType {
    AAP(
        prioritet = 0,
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
        faktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
    ),
    DAGPENGER(
        prioritet = null, // Ikke satt prioritet ennå, ingen stønad gir rett på dagpenger ennå
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
        faktiskMålgruppe = null,
    ),
    OMSTILLINGSSTØNAD(
        prioritet = 5,
        gyldigeAktiviter = setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING),
        faktiskMålgruppe = FaktiskMålgruppe.GJENLEVENDE,
    ),
    OVERGANGSSTØNAD(
        prioritet = 4,
        gyldigeAktiviter = setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING),
        faktiskMålgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER,
    ),
    NEDSATT_ARBEIDSEVNE(
        prioritet = 1,
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
        faktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
    ),
    UFØRETRYGD(
        prioritet = 2,
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
        faktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
    ),
    SYKEPENGER_100_PROSENT(
        prioritet = NULL_IKKE_RETT_PÅ_STØNAD,
        gyldigeAktiviter = emptySet(),
        faktiskMålgruppe = null,
    ),
    INGEN_MÅLGRUPPE(
        prioritet = NULL_IKKE_RETT_PÅ_STØNAD,
        gyldigeAktiviter = emptySet(),
        faktiskMålgruppe = null,
    ),
    ;

    fun prioritet() = prioritet ?: error("Målgruppe=${this.name} har ikke prioritet")

    override fun tilDbType(): String = this.name

    fun gjelderNedsattArbeidsevne() = this == NEDSATT_ARBEIDSEVNE || this == UFØRETRYGD || this == AAP

    fun faktiskMålgruppe() = this.faktiskMålgruppe ?: error("Mangler faktisk målgruppe for $this")

    override fun girIkkeRettPåStønadsperiode() =
        this == INGEN_MÅLGRUPPE ||
            this == SYKEPENGER_100_PROSENT

    fun tilTypeAndel(stønadstype: Stønadstype): TypeAndel =
        when (stønadstype) {
            Stønadstype.BARNETILSYN -> {
                when (this) {
                    AAP, UFØRETRYGD, NEDSATT_ARBEIDSEVNE -> TypeAndel.TILSYN_BARN_AAP
                    OVERGANGSSTØNAD -> TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER
                    OMSTILLINGSSTØNAD -> TypeAndel.TILSYN_BARN_ETTERLATTE
                    else -> error("Kan ikke opprette andel tilkjent ytelse for målgruppe $this")
                }
            }

            Stønadstype.LÆREMIDLER -> {
                when (this) {
                    AAP, UFØRETRYGD, NEDSATT_ARBEIDSEVNE -> TypeAndel.LÆREMIDLER_AAP
                    OVERGANGSSTØNAD -> TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER
                    OMSTILLINGSSTØNAD -> TypeAndel.LÆREMIDLER_ETTERLATTE
                    else -> error("Kan ikke opprette andel tilkjent ytelse for målgruppe $this")
                }
            }

            Stønadstype.BOUTGIFTER -> {
                when (this) {
                    AAP, UFØRETRYGD, NEDSATT_ARBEIDSEVNE -> TypeAndel.BOUTGIFTER_AAP
                    OVERGANGSSTØNAD -> TypeAndel.BOUTGIFTER_ENSLIG_FORSØRGER
                    OMSTILLINGSSTØNAD -> TypeAndel.BOUTGIFTER_ETTERLATTE
                    else -> error("Kan ikke opprette andel tilkjent ytelse for målgruppe $this")
                }
            }
        }

    fun skalVurdereAldersvilkår() =
        when (this) {
            AAP, UFØRETRYGD, NEDSATT_ARBEIDSEVNE, OMSTILLINGSSTØNAD, DAGPENGER -> true
            OVERGANGSSTØNAD, INGEN_MÅLGRUPPE, SYKEPENGER_100_PROSENT -> false
        }
}

private val NULL_IKKE_RETT_PÅ_STØNAD: Int? = null
