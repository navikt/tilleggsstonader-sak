package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørelisteService
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.finnesUkerMedAvvik
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.PrivatBilBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import org.springframework.stereotype.Service

@Service
class KjørelisteSteg(
    private val privatBilBeregningService: PrivatBilBeregningService,
    private val vedtakService: VedtakService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val dagligReiseVedtakService: DagligReiseVedtakService,
    private val avklartKjørelisteService: AvklartKjørelisteService,
    private val satsPrivatBilProvider: SatsPrivatBilProvider,
) : BehandlingSteg<Void?> {
    override fun validerSteg(saksbehandling: Saksbehandling) {
        val avklarteUker = avklartKjørelisteService.hentAvklarteUkerForBehandling(saksbehandling.id)
        brukerfeilHvis(avklarteUker.finnesUkerMedAvvik()) {
            "Kan ikke gå videre til neste steg da det finnes uker med avvik"
        }
    }

    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        val brukersNavKontor =
            if (saksbehandling.stønadstype == Stønadstype.DAGLIG_REISE_TSR) {
                arbeidsfordelingService.hentBrukersNavKontor(saksbehandling.ident)
            } else {
                null
            }

        val eksisterendeVedtak =
            vedtakService.hentVedtak<InnvilgelseEllerOpphørDagligReise>(saksbehandling.id).data

        val rammevedtakPrivatBil =
            eksisterendeVedtak.rammevedtakPrivatBil?.let {
                oppdaterRammevedtakHvisNyeSatser(
                    behandlingId = saksbehandling.id,
                    eksisterendeRammevedtak = it,
                )
            }

        val beregningsresultatPrivatBil =
            privatBilBeregningService.beregn(
                behandling = saksbehandling,
                rammevedtak = rammevedtakPrivatBil,
                beregnFra = eksisterendeVedtak.beregningsplan.beregnFra(),
                brukersNavKontor = brukersNavKontor,
                forrigeBeregningsresultat = hentForrigePrivatBilBeregningsresultat(saksbehandling),
            )

        dagligReiseVedtakService.oppdaterVedtakMedBeregningPrivatBil(
            behandlingId = saksbehandling.id,
            beregningsresultatPrivatBil = beregningsresultatPrivatBil,
        )
    }

    private fun oppdaterRammevedtakHvisNyeSatser(
        behandlingId: BehandlingId,
        eksisterendeRammevedtak: RammevedtakPrivatBil,
    ): RammevedtakPrivatBil {
        val bekreftedeSatser =
            satsPrivatBilProvider.alleSatser
                .filter { it.bekreftet }

        val satserSomSkalOppdateres = eksisterendeRammevedtak.finnSatserSomSkalOppdateres(bekreftedeSatser)

        if (satserSomSkalOppdateres.isEmpty()) return eksisterendeRammevedtak

        val oppdatertRammevedtak = eksisterendeRammevedtak.oppdaterSatser(satserSomSkalOppdateres)

        dagligReiseVedtakService.oppdaterVedtakMedNyttRammevedtakPrivatBil(
            behandlingId = behandlingId,
            rammevedtakPrivatBil = oppdatertRammevedtak,
        )

        return oppdatertRammevedtak
    }

    private fun hentForrigePrivatBilBeregningsresultat(saksbehandling: Saksbehandling): BeregningsresultatPrivatBil? =
        saksbehandling.forrigeIverksatteBehandlingId
            ?.let { forrigeBehandlingId ->
                vedtakService
                    .hentVedtak<InnvilgelseEllerOpphørDagligReise>(forrigeBehandlingId)
                    .data
                    .beregningsresultat
                    .privatBil
            }

    override fun stegType(): StegType = StegType.KJØRELISTE
}
