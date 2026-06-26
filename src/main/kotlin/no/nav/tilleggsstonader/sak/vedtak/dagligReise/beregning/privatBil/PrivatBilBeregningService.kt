package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørelisteService
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtDag
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtDagStatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUkeStatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.GodkjentGjennomførtKjøring
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.finnDagerInnenforPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.avrundetStønadsbeløp
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilDag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBilDelperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PrivatBilBeregningService(
    private val avklartKjørelisteService: AvklartKjørelisteService,
) {
    fun beregn(
        behandling: Saksbehandling,
        rammevedtak: RammevedtakPrivatBil?,
        beregnFra: LocalDate?,
        brukersNavKontor: String?,
        forrigeBeregningsresultat: BeregningsresultatPrivatBil?,
    ): BeregningsresultatPrivatBil? {
        if (rammevedtak == null) return null

        val avklarteUkerForBehandling = avklartKjørelisteService.hentAvklarteUkerForBehandling(behandling.id)

        return beregn(
            rammevedtak = rammevedtak,
            avklarteUkerForBehandling = avklarteUkerForBehandling,
            brukersNavKontor = brukersNavKontor,
            forrigeBeregningsresultat = forrigeBeregningsresultat,
            behandlingType = behandling.type,
            beregnFra = beregnFra,
        )
    }

    private fun beregn(
        rammevedtak: RammevedtakPrivatBil,
        avklarteUkerForBehandling: Collection<AvklartKjørtUke>,
        brukersNavKontor: String?,
        forrigeBeregningsresultat: BeregningsresultatPrivatBil? = null,
        behandlingType: BehandlingType,
        beregnFra: LocalDate?,
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
                    } else if (behandlingType == BehandlingType.REVURDERING) {
                        lagBeregningsresultatForReiseVedRevurderingAvRammevedtak(
                            rammeForReise = reise,
                            avklarteUkerForReise = avklarteUkerForReise,
                            brukersNavKontor = brukersNavKontor,
                            beregnFra = beregnFra,
                            forrigeReise = forrigeReise,
                        )
                    } else if (behandlingType == BehandlingType.KJØRELISTE) {
                        lagBeregningsresultatForReiseVedRevurderingAvKjøreliste(
                            rammeForReise = reise,
                            avklarteUkerForReise = avklarteUkerForReise,
                            brukersNavKontor = brukersNavKontor,
                            forrigeReise = forrigeReise,
                        )
                    } else {
                        feil("Burde ikke være mulig å havne her.")
                    }
                },
        )

    private fun lagBeregningsresultatForReiseVedRevurderingAvKjøreliste(
        rammeForReise: RammevedtakForReiseMedPrivatBil,
        avklarteUkerForReise: List<AvklartKjørtUke>,
        brukersNavKontor: String?,
        forrigeReise: BeregningsresultatForReisePrivatBil,
    ): BeregningsresultatForReisePrivatBil {
        val ukerSomSkalBeregnes =
            avklarteUkerForReise.filter { it.avklartKjørtUkeStatus != AvklartKjørtUkeStatus.UENDRET }
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
        rammeForReise: RammevedtakForReiseMedPrivatBil,
        avklarteUkerForReise: List<AvklartKjørtUke>,
        brukersNavKontor: String?,
    ): BeregningsresultatForReisePrivatBil {
        val avklarteDagerSomSkalBeregnes =
            avklarteUkerForReise
                .flatMap { it.dager }
                .filter { it.avklartKjørtDagStatus != AvklartKjørtDagStatus.SLETTET }

        validerDagerErInnenforRammevedtak(
            rammeForReise = rammeForReise,
            avklarteDagerForReise = avklarteDagerSomSkalBeregnes,
        )

        return BeregningsresultatForReisePrivatBil(
            reiseId = rammeForReise.reiseId,
            perioder =
                rammeForReise.grunnlag.delperioder.flatMap { delperiode ->
                    val avklarteDagerIDelperiode = avklarteDagerSomSkalBeregnes.finnDagerInnenforPeriode(delperiode)
                    lagPerioderForDagerMedSammeSats(
                        dager = avklarteDagerIDelperiode,
                        delperiode = delperiode,
                        brukersNavKontor = brukersNavKontor,
                    )
                },
        )
    }

    /**
     * Dersom rammevedtaket er endret og en reise treffer eller er etter tidligsteEndring reberegnes hele reisen,
     * uavhengig av status på ukene.
     * Dette sikrer at endring i f.eks. bompenger eller ferge plukkes opp.
     */
    private fun lagBeregningsresultatForReiseVedRevurderingAvRammevedtak(
        rammeForReise: RammevedtakForReiseMedPrivatBil,
        avklarteUkerForReise: List<AvklartKjørtUke>,
        brukersNavKontor: String?,
        forrigeReise: BeregningsresultatForReisePrivatBil,
        beregnFra: LocalDate?,
    ): BeregningsresultatForReisePrivatBil {
        feilHvis(beregnFra == null) {
            "Forventer at beregnFra er satt i en revurdering"
        }

        if (rammeForReise.grunnlag.tom < beregnFra && forrigeReise.perioder.maxOf { it.tom } < beregnFra) {
            return forrigeReise.markerAllePerioderSomFraTidligereVedtak()
        }

        return lagBeregningsresultatForReise(
            rammeForReise = rammeForReise,
            avklarteUkerForReise = avklarteUkerForReise,
            brukersNavKontor = brukersNavKontor,
        )
    }

    private fun validerDagerErInnenforRammevedtak(
        rammeForReise: RammevedtakForReiseMedPrivatBil,
        avklarteDagerForReise: List<AvklartKjørtDag>,
    ) {
        avklarteDagerForReise
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
