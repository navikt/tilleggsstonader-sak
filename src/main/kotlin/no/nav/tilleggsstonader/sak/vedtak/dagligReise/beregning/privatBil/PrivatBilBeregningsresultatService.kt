package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtDag
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUkeStatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.GodkjentGjennomførtKjøring
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.avrundetStønadsbeløp
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilDag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBilDelperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import org.springframework.stereotype.Service

@Service
class PrivatBilBeregningsresultatService {
    fun beregn(
        rammevedtak: RammevedtakPrivatBil,
        avklarteUkerForBehandling: Collection<AvklartKjørtUke>,
        brukersNavKontor: String?,
        forrigeBeregningsresultat: BeregningsresultatPrivatBil? = null,
    ): BeregningsresultatPrivatBil =
        BeregningsresultatPrivatBil(
            reiser =
                rammevedtak.reiser.map { reise ->
                    val avklarteUkerForReise = avklarteUkerForBehandling.filter { it.reiseId == reise.reiseId }
                    val forrigeReise = forrigeBeregningsresultat?.reiser?.find { it.reiseId == reise.reiseId }

                    if (forrigeReise == null) {
                        lagBeregningsresultatForReise(
                            rammeForReise = reise,
                            avklarteUkerForReise = avklarteUkerForReise,
                            brukersNavKontor = brukersNavKontor,
                        )
                    } else {
                        lagBeregningsresultatForReiseVedRevurdering(
                            rammeForReise = reise,
                            avklarteUkerForReise = avklarteUkerForReise,
                            brukersNavKontor = brukersNavKontor,
                            forrigeReise = forrigeReise,
                        )
                    }
                },
        )

    private fun lagBeregningsresultatForReiseVedRevurdering(
        rammeForReise: RammeForReiseMedPrivatBil,
        avklarteUkerForReise: List<AvklartKjørtUke>,
        brukersNavKontor: String?,
        forrigeReise: BeregningsresultatForReisePrivatBil,
    ): BeregningsresultatForReisePrivatBil {
        val ukerSomSkalBeregnes = avklarteUkerForReise.filter { it.avklartKjørtUkeStatus != AvklartKjørtUkeStatus.UENDRET }
        val ukerSomSkalGjenbrukes =
            avklarteUkerForReise.filter {
                it.avklartKjørtUkeStatus == AvklartKjørtUkeStatus.UENDRET
            }

        val nyBeregnedePerioder =
            lagBeregningsresultatForReise(
                rammeForReise = rammeForReise,
                avklarteUkerForReise = ukerSomSkalBeregnes,
                brukersNavKontor = brukersNavKontor,
            ).perioder

        val gjenbruktePerioder =
            ukerSomSkalGjenbrukes.map { uke ->
                val periode =
                    forrigeReise.perioder.find { it.fom.tilUkeIÅr() == uke.uke }
                        ?: error("Fant ikke periode for uke ${uke.uke} i forrige vedtak, men uke har status UENDRET")
                periode.copy(fraTidligereVedtak = true)
            }

        return BeregningsresultatForReisePrivatBil(
            reiseId = rammeForReise.reiseId,
            perioder = (nyBeregnedePerioder + gjenbruktePerioder).sortedBy { it.fom },
        )
    }

    private fun lagBeregningsresultatForReise(
        rammeForReise: RammeForReiseMedPrivatBil,
        avklarteUkerForReise: List<AvklartKjørtUke>,
        brukersNavKontor: String?,
    ): BeregningsresultatForReisePrivatBil {
        // Kaster feil om det finnes godkjente dager utenfor rammevedtak
        validerDagerErInnenforRammevedtak(rammeForReise, avklarteUkerForReise)
        val delperioder = rammeForReise.grunnlag.delperioder

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
                                dagsatsUtenParkering = dagsatsUtenParkering.setScale(2),
                                stønadsbeløpForDag =
                                    dagsatsUtenParkering
                                        .plus(parkeringsutgift.toBigDecimal())
                                        .setScale(2),
                            )
                        }

                BeregningsresultatForReisePrivatBilPeriode(
                    fom = dager.minOf { it.dato },
                    tom = dager.maxOf { it.dato },
                    grunnlag =
                        BeregningsresultatForReisePrivatBilGrunnlag(
                            dager = beregnedeDager,
                        ),
                    stønadsbeløp = beregnedeDager.sumOf { it.stønadsbeløpForDag }.avrundetStønadsbeløp(),
                    brukersNavKontor = brukersNavKontor,
                    fraTidligereVedtak = false,
                )
            }
    }
}
