package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel

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
                error("Mappingen til TypeAndel for stønadstype boutgifter er ikke implementert")
            }
        }
}

private val NULL_IKKE_RETT_PÅ_STØNAD: Int? = null
