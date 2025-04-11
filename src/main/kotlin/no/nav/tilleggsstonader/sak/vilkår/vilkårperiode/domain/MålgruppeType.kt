package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe

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

    override fun girIkkeRettPåVedtaksperiode() =
        this == INGEN_MÅLGRUPPE ||
            this == SYKEPENGER_100_PROSENT

    fun skalVurdereAldersvilkår() =
        when (this) {
            AAP, UFØRETRYGD, NEDSATT_ARBEIDSEVNE, OMSTILLINGSSTØNAD, DAGPENGER -> true
            OVERGANGSSTØNAD, INGEN_MÅLGRUPPE, SYKEPENGER_100_PROSENT -> false
        }
}

private val NULL_IKKE_RETT_PÅ_STØNAD: Int? = null
