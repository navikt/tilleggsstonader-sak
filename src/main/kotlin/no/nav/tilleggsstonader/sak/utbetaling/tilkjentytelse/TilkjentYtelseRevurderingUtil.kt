package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse

import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
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

    /**
     * Dette vil ikke virke så bra. Dagytelser har utbetaling av hele beløpet på 1 dag.
     * Så når man gjør en revurder fra vill ikke dette virke veldig bra...
     */
    fun gjenbrukAndelerFraForrigeTilkjentYtelse(
        tilkjentYtelse: TilkjentYtelse,
        revurderFra: LocalDate,
    ): List<AndelTilkjentYtelse> {
        val revurderFraMåned = YearMonth.from(revurderFra).atDay(1)
        return tilkjentYtelse.andelerTilkjentYtelse
            .filter { andel ->
                feilHvis(andel.fom != andel.tom) {
                    "Håndterer foreløpig ikke at fom != tom"
                }
                andel.tom < revurderFraMåned
            }
            .map {
                // TODO hvordan håndtere ugyldig satstype/type
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
}
