package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørelisteService
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.finnesUkerMedAvvik
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.PrivatBilBeregningsresultatService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import org.springframework.stereotype.Service

@Service
class KjørelisteSteg(
    private val behandlingService: BehandlingService,
    private val privatBilBeregningsresultatService: PrivatBilBeregningsresultatService,
    private val vedtakService: VedtakService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val dagligReiseVedtakService: DagligReiseVedtakService,
    private val avklartKjørelisteService: AvklartKjørelisteService,
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
                arbeidsfordelingService.hentBrukersNavKontor(saksbehandling.ident, saksbehandling.stønadstype)
            } else {
                null
            }

        val eksisterendeRammevedtak =
            vedtakService.hentVedtak<InnvilgelseDagligReise>(saksbehandling.id).data.rammevedtakPrivatBil
                ?: error("Finner ikke rammevedtak for behandling ${saksbehandling.id}")
        val beregningsresultatPrivatBil =
            privatBilBeregningsresultatService.beregn(
                rammevedtak = eksisterendeRammevedtak,
                avklarteUkerForBehandling = avklartKjørelisteService.hentAvklarteUkerForBehandling(saksbehandling.id),
                brukersNavKontor = brukersNavKontor,
                forrigeBeregningsresultat = hentForrigePrivatBilBeregningsresultat(saksbehandling),
            )

        dagligReiseVedtakService.oppdaterVedtakMedBeregningPrivatBil(
            behandlingId = saksbehandling.id,
            beregningsresultatPrivatBil = beregningsresultatPrivatBil,
        )
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
