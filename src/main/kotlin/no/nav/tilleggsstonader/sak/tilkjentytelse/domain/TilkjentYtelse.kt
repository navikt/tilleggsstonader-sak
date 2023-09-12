package no.nav.tilleggsstonader.sak.tilkjentytelse.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

/**
 * @param startdato dato for når vi tidligst er ansvarlige for nye perioder.
 * Når denne er satt kan den aldri flyttes fremover i tid, ettersom vi må ta ansvar for det tidspunktet vi behandlet saken fra.
 *  Den kan endres tilbake i tid hvis vi gjør vedtak med en dato før forrige startdato.
 * I en førstegangsbehandling så settes startdato til den første andelens startdato
 * Når man revurderer så settes startdato i den nye til det tidligste av:
 *   forrige startdato, tidligste startdato på nye andeler, eller opphørsdato dersom denne er før forrige startdato
 *
 * Startdato brukes for å sende over korrekte utbetalingsoppdrag til økonomi.
 * For migrerte saker er startdato en viktig indikator på når dette systemet har overtatt utbetalingsansvaret.
 * All utbetaling før startdato vil i så fall finnes i andre systemer.
 *
 * Startdato brukes også når vi skal slå sammen perioder tvers Arena og tilleggstonader,
 * der vi bruker startdato for å avkorte perioder fra Arena, spesielt i de tilfelle der startdato er før laveste fom-dato for en andel
 */
data class TilkjentYtelse(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    val startdato: LocalDate,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {

    fun taMedAndelerFremTilDato(fom: LocalDate): List<AndelTilkjentYtelse> = andelerTilkjentYtelse
        .filter { andel -> andel.periode.fomDato < fom }
        .map { andel ->
            if (andel.erStønadOverlappende(fom)) {
                andel.copy(stønadTom = fom.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()))
            } else {
                andel
            }
        }
}
