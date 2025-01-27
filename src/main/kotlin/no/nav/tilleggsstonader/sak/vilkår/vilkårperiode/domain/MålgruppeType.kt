package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

/**
 * @param prioritet lavest er den som har høyest prioritet
 */
enum class MålgruppeType(
    private val prioritet: Int?,
    val gyldigeAktiviter: Set<AktivitetType>,
) : VilkårperiodeType {
    AAP(
        prioritet = 0,
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
    ),
    DAGPENGER(
        prioritet = null, // Ikke satt prioritet ennå, ingen stønad gir rett på dagpenger ennå
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
    ),
    OMSTILLINGSSTØNAD(
        prioritet = 5,
        gyldigeAktiviter = setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING),
    ),
    OVERGANGSSTØNAD(
        prioritet = 4,
        gyldigeAktiviter = setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING),
    ),
    NEDSATT_ARBEIDSEVNE(
        prioritet = 1,
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
    ),
    UFØRETRYGD(
        prioritet = 2,
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
    ),
    SYKEPENGER_100_PROSENT(
        prioritet = NULL_IKKE_RETT_PÅ_STØNAD,
        gyldigeAktiviter = emptySet(),
    ),
    INGEN_MÅLGRUPPE(
        prioritet = NULL_IKKE_RETT_PÅ_STØNAD,
        gyldigeAktiviter = emptySet(),
    ),
    ;

    fun prioritet() = prioritet ?: error("Målgruppe=${this.name} har ikke prioritet")

    override fun tilDbType(): String = this.name

    fun gjelderNedsattArbeidsevne() = this == NEDSATT_ARBEIDSEVNE || this == UFØRETRYGD || this == AAP

    override fun girIkkeRettPåStønadsperiode() =
        this == INGEN_MÅLGRUPPE ||
            this == SYKEPENGER_100_PROSENT
}

private val NULL_IKKE_RETT_PÅ_STØNAD: Int? = null
