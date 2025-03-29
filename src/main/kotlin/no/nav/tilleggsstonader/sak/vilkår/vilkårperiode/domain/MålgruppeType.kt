package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.domain.FaktiskMålgruppe

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
        faktiskMålgruppe = FaktiskMålgruppe.AAP,
    ),
    DAGPENGER(
        prioritet = null, // Ikke satt prioritet ennå, ingen stønad gir rett på dagpenger ennå
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
        faktiskMålgruppe = null,
    ),
    OMSTILLINGSSTØNAD(
        prioritet = 5,
        gyldigeAktiviter = setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING),
        faktiskMålgruppe = FaktiskMålgruppe.OMSTILLINGSSTØNAD,
    ),
    OVERGANGSSTØNAD(
        prioritet = 4,
        gyldigeAktiviter = setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING),
        faktiskMålgruppe = FaktiskMålgruppe.OVERGANGSSTØNAD,
    ),
    NEDSATT_ARBEIDSEVNE(
        prioritet = 1,
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
        faktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
    ),
    UFØRETRYGD(
        prioritet = 2,
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
        faktiskMålgruppe = FaktiskMålgruppe.UFØRETRYGD,
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

    fun faktiskMålgruppe() = faktiskMålgruppe ?: error("Målgruppe=${this.name} har ikke faktisk målgruppe")

    override fun tilDbType(): String = this.name

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
}

private val NULL_IKKE_RETT_PÅ_STØNAD: Int? = null
