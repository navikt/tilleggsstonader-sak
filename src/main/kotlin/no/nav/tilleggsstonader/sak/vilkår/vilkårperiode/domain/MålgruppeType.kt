package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

enum class MålgruppeType(val faktiskMålgruppe: FaktiskMålgruppe, val gyldigeAktiviter: Set<AktivitetType>) :
    VilkårperiodeType {
    AAP(
        faktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
    ),
    DAGPENGER(
        faktiskMålgruppe = FaktiskMålgruppe.INGEN_MÅLGRUPPE,
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
    ),
    OMSTILLINGSSTØNAD(
        faktiskMålgruppe = FaktiskMålgruppe.GJENLEVENDE,
        gyldigeAktiviter = setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING),
    ),
    OVERGANGSSTØNAD(
        faktiskMålgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER,
        gyldigeAktiviter = setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING),
    ),
    NEDSATT_ARBEIDSEVNE(
        faktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
    ),
    UFØRETRYGD(
        faktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
    ),
    SYKEPENGER_100_PROSENT(
        faktiskMålgruppe = FaktiskMålgruppe.INGEN_MÅLGRUPPE,
        gyldigeAktiviter = emptySet(),
    ),
    INGEN_MÅLGRUPPE(
        faktiskMålgruppe = FaktiskMålgruppe.INGEN_MÅLGRUPPE,
        gyldigeAktiviter = emptySet(),
    ),
    ;

    override fun tilDbType(): String = this.name

    fun gjelderNedsattArbeidsevne() = this == NEDSATT_ARBEIDSEVNE || this == UFØRETRYGD || this == AAP

    override fun girIkkeRettPåStønadsperiode() =
        this == INGEN_MÅLGRUPPE ||
            this == SYKEPENGER_100_PROSENT
}

enum class FaktiskMålgruppe {
    NEDSATT_ARBEIDSEVNE,
    ENSLIG_FORSØRGER,
    GJENLEVENDE,
    INGEN_MÅLGRUPPE,
}
