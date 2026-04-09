package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtDag
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.GodkjentGjennomførtKjøring
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilDag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBilDelperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PrivatBilBeregningsresultatService {
    fun beregn(
        rammevedtak: RammevedtakPrivatBil,
        avklarteUkerForBehandling: Collection<AvklartKjørtUke>,
        brukersNavKontor: String?,
    ): BeregningsresultatPrivatBil =
        BeregningsresultatPrivatBil(
            reiser =
                rammevedtak.reiser.map { reise ->
                    lagBeregningsresultatForReise(
                        rammeForReise = reise,
                        avklarteUkerForReise = avklarteUkerForBehandling.filter { it.reiseId == reise.reiseId },
                        brukersNavKontor = brukersNavKontor,
                    )
                },
        )

    private fun lagBeregningsresultatForReise(
        rammeForReise: RammeForReiseMedPrivatBil,
        avklarteUkerForReise: List<AvklartKjørtUke>,
        brukersNavKontor: String?,
    ): BeregningsresultatForReisePrivatBil {
        // Kaster feil om det finnes godkjente dager utenfor rammevedtak
        validerDagerErInnenforRammevedtak(rammeForReise, avklarteUkerForReise)
        val delperioder = rammeForReise.grunnlag.delPerioder

        return BeregningsresultatForReisePrivatBil(
            reiseId = rammeForReise.reiseId,
            perioder =
                delperioder.flatMap { delperiode ->
                    val dagerForDelperiode =
                        avklarteUkerForReise
                            .flatMap { it.dager }
                            .filter { dag ->
                                rammeForReise.grunnlag.inneholder(dag.dato) &&
                                    delperiode.fom <= dag.dato && dag.dato <= delperiode.tom
                            }
                    lagPerioderForDagerMedSammeSats(
                        dagerForDelperiode,
                        delperiode,
                        brukersNavKontor,
                    )
                },
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
        delperiode: RammeForReiseMedPrivatBilDelperiode,
        brukersNavKontor: String?,
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
                            val dagsatsUtenParkering = delperiode.finnSatsForDato(dag.dato).dagsatsUtenParkering
                            BeregningsresultatForReisePrivatBilDag(
                                dato = dag.dato,
                                parkeringskostnad = parkeringsutgift,
                                dagsatsUtenParkering = dagsatsUtenParkering,
                                stønadsbeløpForDag = dagsatsUtenParkering.plus(parkeringsutgift.toBigDecimal()),
                            )
                        }

                BeregningsresultatForReisePrivatBilPeriode(
                    fom = dager.minOf { it.dato },
                    tom = dager.maxOf { it.dato },
                    grunnlag =
                        BeregningsresultatForReisePrivatBilGrunnlag(
                            dager = beregnedeDager,
                        ),
                    stønadsbeløp = beregnedeDager.sumOf { it.stønadsbeløpForDag },
                    brukersNavKontor = brukersNavKontor,
                )
            }
    }
}

private fun List<RammeForReiseMedPrivatBilDelperiode>.delperiodeForDato(dato: LocalDate) =
    singleOrNull { it.fom <= dato && dato <= it.tom }
        ?: error("Finner ingen delperiode i rammevedtak for dato $dato")
