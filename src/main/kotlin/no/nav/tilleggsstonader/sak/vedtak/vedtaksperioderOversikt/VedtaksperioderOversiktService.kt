package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.detaljerteVedtaksperioder.DetaljertVedtaksperiodeTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.detaljerteVedtaksperioder.DetaljertVedtaksperioderTilsynBarnMapper.finnDetaljerteVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.detaljerteVedtaksperioder.DetaljertVedtaksperiodeBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.detaljerteVedtaksperioder.DetaljertVedtaksperioderBoutgifterMapper.finnDetaljerteVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder.DetaljertVedtaksperiodeDagligReiseTso
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder.DetaljertVedtaksperiodeDagligReiseTsr
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksdata
import no.nav.tilleggsstonader.sak.vedtak.læremidler.detaljerteVedtaksperioder.DetaljertVedtaksperiodeLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.detaljerteVedtaksperioder.DetaljertVedtaksperioderLæremidlerMapper.finnDetaljerteVedtaksperioder
import org.springframework.stereotype.Service

@Service
class VedtaksperioderOversiktService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
) {
    /**
     * Oversikten baserer seg på vedtaksperiodene fra beregningsresultatet, som inneholder mer
     * detaljert informasjon spesifikk for stønadstypen.
     * De er derfor ikke nødvendigvis en til en med vedtaksperiodene som saksbehandler registrerer.
     */
    fun hentVedtaksperioderOversikt(fagsakPersonId: FagsakPersonId): VedtaksperioderOversikt {
        val fagsaker = fagsakService.finnFagsakerForFagsakPersonId(fagsakPersonId)

        return VedtaksperioderOversikt(
            tilsynBarn = fagsaker.barnetilsyn?.let { oppsummerVedtaksperioderTilsynBarn(it.id) } ?: emptyList(),
            læremidler = fagsaker.læremidler?.let { oppsummerVedtaksperioderLæremidler(it.id) } ?: emptyList(),
            boutgifter = fagsaker.boutgifter?.let { oppsummerVedtaksperioderBoutgifter(it.id) } ?: emptyList(),
            dagligReiseTso =
                fagsaker.dagligReiseTso?.let { oppsummerVedtaksperioderDagligReiseTso(it.id) }
                    ?: emptyList(),
            dagligReiseTsr =
                fagsaker.dagligReiseTsr?.let { oppsummerVedtaksperioderDagligReiseTsr(it.id) }
                    ?: emptyList(),
        )
    }

    fun hentDetaljerteVedtaksperioderForBehandlingBoutgifter(behandlingId: BehandlingId): List<DetaljertVedtaksperiodeBoutgifter>? {
        val vedtak = vedtakService.hentVedtak<InnvilgelseEllerOpphørBoutgifter>(behandlingId) ?: return null
        return vedtak.data.finnDetaljerteVedtaksperioder()
    }

    private fun oppsummerVedtaksperioderTilsynBarn(fagsakId: FagsakId): List<DetaljertVedtaksperiodeTilsynBarn> {
        val vedtakForSisteIverksatteBehandling =
            hentVedtaksdataForSisteIverksatteBehandling<InnvilgelseEllerOpphørTilsynBarn>(fagsakId)
                ?: return emptyList()

        return vedtakForSisteIverksatteBehandling.finnDetaljerteVedtaksperioder()
    }

    private fun oppsummerVedtaksperioderLæremidler(fagsakId: FagsakId): List<DetaljertVedtaksperiodeLæremidler> {
        val vedtakForSisteIverksatteBehandling =
            hentVedtaksdataForSisteIverksatteBehandling<InnvilgelseEllerOpphørLæremidler>(fagsakId)
                ?: return emptyList()

        return vedtakForSisteIverksatteBehandling.finnDetaljerteVedtaksperioder()
    }

    private fun oppsummerVedtaksperioderBoutgifter(fagsakId: FagsakId): List<DetaljertVedtaksperiodeBoutgifter> {
        val vedtakForSisteIverksatteBehandling =
            hentVedtaksdataForSisteIverksatteBehandling<InnvilgelseEllerOpphørBoutgifter>(fagsakId)
                ?: return emptyList()

        return vedtakForSisteIverksatteBehandling.finnDetaljerteVedtaksperioder()
    }

    private fun oppsummerVedtaksperioderDagligReiseTso(fagsakId: FagsakId): List<DetaljertVedtaksperiodeDagligReiseTso> = emptyList()

    private fun oppsummerVedtaksperioderDagligReiseTsr(fagsakId: FagsakId): List<DetaljertVedtaksperiodeDagligReiseTsr> = emptyList()

    private inline fun <reified T : Vedtaksdata> hentVedtaksdataForSisteIverksatteBehandling(fagsakId: FagsakId): T? {
        val sisteIverksatteBehandling = behandlingService.finnSisteIverksatteBehandling(fagsakId) ?: return null
        val vedtak = vedtakService.hentVedtak<T>(sisteIverksatteBehandling.id) ?: return null

        return vedtak.data
    }
}
