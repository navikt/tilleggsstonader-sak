package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

enum class MålgruppeType(val gyldigeAktiviter: Set<AktivitetType>) : VilkårperiodeType {
    AAP(setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING)),
    DAGPENGER(setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING)),
    OMSTILLINGSSTØNAD(setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING)),
    OVERGANGSSTØNAD(setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING)),
    NEDSATT_ARBEIDSEVNE(setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING)),
    UFØRETRYGD(setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING)),
    SYKEPENGER_100_PROSENT(emptySet()),
    INGEN_MÅLGRUPPE(emptySet()),
    ;

    override fun tilDbType(): String = this.name

    fun gjelderNedsattArbeidsevne() = this == NEDSATT_ARBEIDSEVNE || this == UFØRETRYGD || this == AAP

    override fun girIkkeRettPåStønadsperiode() =
        this == INGEN_MÅLGRUPPE ||
            this == SYKEPENGER_100_PROSENT
}

data class DelvilkårMålgruppe(
    val medlemskap: Vurdering,
    val dekketAvAnnetRegelverk: Vurdering,
) : DelvilkårVilkårperiode()