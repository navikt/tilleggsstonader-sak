package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtDag
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.GodkjentGjennomførtKjøring
import no.nav.tilleggsstonader.sak.util.finnMandagNesteUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilDag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.SatsForPeriodePrivatBil
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PrivatBilBeregningsresultatService {
    fun beregn(
        rammevedtak: RammevedtakPrivatBil,
        avklarteUkerForBehandling: Collection<AvklartKjørtUke>,
    ): BeregningsresultatPrivatBil? =
        BeregningsresultatPrivatBil(
            reiser =
                rammevedtak.reiser.map { reise ->
                    lagBeregningsresultatForReise(
                        rammeForReise = reise,
                        avklarteUkerForReise = avklarteUkerForBehandling.filter { it.reiseId == reise.reiseId },
                    )
                },
        )

    private fun lagBeregningsresultatForReise(
        rammeForReise: RammeForReiseMedPrivatBil,
        avklarteUkerForReise: List<AvklartKjørtUke>,
    ): BeregningsresultatForReisePrivatBil {
        // Kaster feil om det finnes godkjente dager utenfor rammevedtak
        validerDagerErInnenforRammevedtak(rammeForReise, avklarteUkerForReise)
        val satser = rammeForReise.grunnlag.satser

        // Grupper dager på sats
        return BeregningsresultatForReisePrivatBil(
            reiseId = rammeForReise.reiseId,
            perioder =
                avklarteUkerForReise
                    .flatMap { it.dager }
                    .filter { rammeForReise.grunnlag.inneholder(it.dato) }
                    .groupBy { satser.satsForDato(it.dato) }
                    .flatMap { (sats, dager) -> lagPerioderForDagerMedSammeSats(dager, sats) },
        )
    }

    private fun validerDagerErInnenforRammevedtak(
        rammeForReise: RammeForReiseMedPrivatBil,
        avklarteUkerForReise: List<AvklartKjørtUke>,
    ) {
        avklarteUkerForReise
            .flatMap { it.dager }
            .filter { it.godkjentGjennomførtKjøring == GodkjentGjennomførtKjøring.JA }
            .forEach {
                feilHvis(!rammeForReise.grunnlag.inneholder(it.dato)) {
                    "Dag ${it.dato} er ikke innenfor rammevedtak (${rammeForReise.grunnlag.fom} - ${rammeForReise.grunnlag.tom})"
                }
            }
    }

    private fun lagPerioderForDagerMedSammeSats(
        dager: List<AvklartKjørtDag>,
        sats: SatsForPeriodePrivatBil,
    ): Collection<BeregningsresultatForReisePrivatBilPeriode> {
        // Grupper dager på uke, slik at alle dager innenfor en uke utbetales samme dag
        return dager
            .groupBy { it.dato.tilUkeIÅr() }
            .map { (_, dager) ->
                val beregnedeDager =
                    dager
                        .filter { dag -> dag.godkjentGjennomførtKjøring == GodkjentGjennomførtKjøring.JA }
                        .map { dag ->
                            val parkeringsutgift = dag.parkeringsutgift ?: 0
                            BeregningsresultatForReisePrivatBilDag(
                                dato = dag.dato,
                                parkeringskostnad = parkeringsutgift,
                                stønadsbeløpForDag = sats.dagsatsUtenParkering.plus(parkeringsutgift.toBigDecimal()),
                            )
                        }

                BeregningsresultatForReisePrivatBilPeriode(
                    fom = dager.minOf { it.dato },
                    tom = dager.maxOf { it.dato },
                    utbetalingsdato = dager.minOf { it.dato }.finnMandagNesteUke(), // Utbetalingsdato mandag uka etter
                    grunnlag =
                        BeregningsresultatForReisePrivatBilGrunnlag(
                            dager = beregnedeDager,
                            dagsatsUtenParkering = sats.dagsatsUtenParkering,
                        ),
                    stønadsbeløp = beregnedeDager.sumOf { it.stønadsbeløpForDag },
                )
            }
    }
}

private fun List<SatsForPeriodePrivatBil>.satsForDato(dato: LocalDate) =
    singleOrNull { it.inneholder(dato) }
        ?: error("Finner ingen sats i rammevedtak for dato $dato")
