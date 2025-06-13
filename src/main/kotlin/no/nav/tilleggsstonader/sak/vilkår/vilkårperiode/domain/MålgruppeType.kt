package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

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
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
        faktiskMålgruppe = null,
    ),
    INGEN_MÅLGRUPPE(
        gyldigeAktiviter = emptySet(),
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
            AAP, UFØRETRYGD, NEDSATT_ARBEIDSEVNE, OMSTILLINGSSTØNAD, DAGPENGER, TILTAKSPENGER -> true
            OVERGANGSSTØNAD, INGEN_MÅLGRUPPE, SYKEPENGER_100_PROSENT -> false
        }
}
