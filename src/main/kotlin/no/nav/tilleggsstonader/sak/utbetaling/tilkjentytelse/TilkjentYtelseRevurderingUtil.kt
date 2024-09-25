package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import java.time.LocalDate
import java.time.YearMonth

object TilkjentYtelseRevurderingUtil {

    fun validerNyeAndelerBegynnerEtterRevurderFra(
        saksbehandling: Saksbehandling,
        andeler: List<AndelTilkjentYtelse>,
    ) {
        val revurderFra = saksbehandling.revurderFra ?: return
        val andelerSomBegynnerFørRevurderFra = andeler.filter { it.fom < revurderFra }
        feilHvis(andelerSomBegynnerFørRevurderFra.isNotEmpty()) {
            secureLogger.error(
                "Kan ikke opprette andeler som begynner før revurderFra($revurderFra)" +
                    " andeler=$andelerSomBegynnerFørRevurderFra",
            )
            "Kan ikke opprette andeler som begynner før revurderFra"
        }
    }

    fun gjenbrukAndelerFraForrigeTilkjentYtelse(
        saksbehandling: Saksbehandling,
        tilkjentYtelse: TilkjentYtelse,
        revurderFra: LocalDate,
    ): List<AndelTilkjentYtelse> {
        val revurderFraMåned = YearMonth.from(revurderFra).atDay(1)

        validerGjenbruk(saksbehandling)
        validerAndeler(tilkjentYtelse.andelerTilkjentYtelse)

        return tilkjentYtelse.andelerTilkjentYtelse
            .filterNot { andel -> andel.type == TypeAndel.UGYLDIG }
            .filter { andel -> andel.tom < revurderFraMåned }
            .map {
                AndelTilkjentYtelse(
                    beløp = it.beløp,
                    fom = it.fom,
                    tom = it.tom,
                    satstype = it.satstype,
                    type = it.type,
                    kildeBehandlingId = it.kildeBehandlingId,
                )
            }
    }

    /**
     * For tilsyn barn har man lagd andeler som har lik fom og tom, der hele beløpet for en periode legges på et dato.
     * Dette er på grunn av aktivitetsdager der det er utydelig lengde på perioden.
     * Eks man kan ha et tiltak 1.8-31.8 med 1 aktivitetsdag, man får då utbetalt 1 dag per uke, dvs 4 dager i august.
     *
     * Hvis man revurderer fra 15 aug, og endrer aktivitetsdager så må man korrigere utbetalingen
     * som er satt på 1.8 fra 4 til 2 dager, og ev legge inn en ny andel fra og med 15 aug.
     * Eks der man endrer til 4 aktivitetsdager fra 15 aug (10kr per innvilget dag)
     * Behandling 1: 1.8 aug 4 dager, 40kr
     * Behandling 2: 1.8 aug 2 dager, 20kr. 15 aug 4 dager, 40kr
     *
     * For andre stønader har man kanskje annet behov og laget andeler på annen måte. Må av den grunnen ta stilling til hvert enkelt tilfelle,
     */
    private fun validerGjenbruk(saksbehandling: Saksbehandling) {
        feilHvis(saksbehandling.stønadstype != Stønadstype.BARNETILSYN) {
            "Har ikke tatt stilling til hvordan andre stønasdtyper enn barnetilsyn skal gjenbruke andeler"
        }
    }

    /**
     * Koblet til validering av at det kun gjelder [Stønadstype.BARNETILSYN]
     * Se kommentar på [validerGjenbruk]
     */
    private fun validerAndeler(andelerTilkjentYtelse: Set<AndelTilkjentYtelse>) {
        andelerTilkjentYtelse.forEach {
            feilHvis(it.fom != it.tom) {
                "Forventer at andeler har fom=tom"
            }
            feilHvis(it.satstype != Satstype.DAG) {
                "Håndterer ikke type=${it.satstype}"
            }
        }
    }
}
