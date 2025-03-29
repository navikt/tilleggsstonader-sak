package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

/**
 * @param prioritet lavest er den som har høyest prioritet
 */
enum class FaktiskMålgruppe(
    private val prioritet: Int?,
    val gyldigeAktiviter: Set<AktivitetType>,
) {
    AAP(
        prioritet = 0,
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
    ),
    NEDSATT_ARBEIDSEVNE(
        prioritet = 1,
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
    ),
    UFØRETRYGD(
        prioritet = 2,
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
    ),
    OVERGANGSSTØNAD(
        prioritet = 3,
        gyldigeAktiviter = setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING),
    ),
    OMSTILLINGSSTØNAD(
        prioritet = 4,
        gyldigeAktiviter = setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING),
    ),
    ;

    fun prioritet() = prioritet ?: error("Målgruppe=${this.name} har ikke prioritet")

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
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
