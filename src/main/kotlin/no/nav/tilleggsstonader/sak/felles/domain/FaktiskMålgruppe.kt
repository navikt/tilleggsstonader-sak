package no.nav.tilleggsstonader.sak.felles.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType

/**
 * @param prioritet lavest er den som har høyest prioritet
 */
enum class FaktiskMålgruppe(
    private val prioritet: Int?,
    val gyldigeAktiviter: Set<AktivitetType>,
) {
    NEDSATT_ARBEIDSEVNE(
        prioritet = 1,
        gyldigeAktiviter = setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING),
    ),
    ENSLIG_FORSØRGER(
        prioritet = 2,
        gyldigeAktiviter = setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING),
    ),
    GJENLEVENDE(
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
                    NEDSATT_ARBEIDSEVNE -> TypeAndel.TILSYN_BARN_AAP
                    ENSLIG_FORSØRGER -> TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER
                    GJENLEVENDE -> TypeAndel.TILSYN_BARN_ETTERLATTE
                    else -> error("Kan ikke opprette andel tilkjent ytelse for målgruppe $this")
                }
            }
            Stønadstype.LÆREMIDLER -> {
                when (this) {
                    NEDSATT_ARBEIDSEVNE -> TypeAndel.LÆREMIDLER_AAP
                    ENSLIG_FORSØRGER -> TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER
                    GJENLEVENDE -> TypeAndel.LÆREMIDLER_ETTERLATTE
                    else -> error("Kan ikke opprette andel tilkjent ytelse for målgruppe $this")
                }
            }
            Stønadstype.BOUTGIFTER -> {
                when (this) {
                    NEDSATT_ARBEIDSEVNE -> TypeAndel.BOUTGIFTER_AAP
                    ENSLIG_FORSØRGER -> TypeAndel.BOUTGIFTER_ENSLIG_FORSØRGER
                    GJENLEVENDE -> TypeAndel.BOUTGIFTER_ETTERLATTE
                    else -> error("Kan ikke opprette andel tilkjent ytelse for målgruppe $this")
                }
            }
            Stønadstype.DAGLIG_REISE_TSO -> TODO("Daglig reise er ikke implementert enda")
            Stønadstype.DAGLIG_REISE_TSR -> TODO("Daglig reise er ikke implementert enda")
        }
}
