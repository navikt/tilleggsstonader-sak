package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.detaljerteVedtaksperioder.DetaljertVedtaksperiodeTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.detaljerteVedtaksperioder.DetaljertVedtaksperioderTilsynBarnMapper.finnDetaljerteVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.detaljerteVedtaksperioder.DetaljertVedtaksperiodeBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.detaljerteVedtaksperioder.DetaljertVedtaksperioderBoutgifterMapper.finnDetaljerteVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder.DetaljertVedtaksperiodeDagligReiseTso
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder.DetaljertVedtaksperiodeDagligReiseTsr
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder.DetaljertVedtaksperioderDagligReiseMapper.finnDetaljerteVedtaksperioderTso
import no.nav.tilleggsstonader.sak.vedtak.domain.DetaljertVedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
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
            tilsynBarn = fagsaker.barnetilsyn?.let { oppsummerVedtaksperioderTilsynBarn(it.id) },
            læremidler = fagsaker.læremidler?.let { oppsummerVedtaksperioderLæremidler(it.id) },
            boutgifter = fagsaker.boutgifter?.let { oppsummerVedtaksperioderBoutgifter(it.id) },
            dagligReiseTso = fagsaker.dagligReiseTso?.let { oppsummerVedtaksperioderDagligReiseTso(it.id) },
            dagligReiseTsr = fagsaker.dagligReiseTsr?.let { oppsummerVedtaksperioderDagligReiseTsr(it.id) },
        )
    }

    fun hentDetaljerteVedtaksperioderForBehandling(
        behandlingId: BehandlingId,
    ): VedtaksperiodeOversiktForBehandling<out DetaljertVedtaksperiode> {
        val vedtaksdata = vedtakService.hentVedtak(behandlingId)?.data
        feilHvis(vedtaksdata == null) {
            "Behandling med id $behandlingId har ikke vedtak"
        }
        return when (vedtaksdata) {
            is InnvilgelseEllerOpphørTilsynBarn -> lagVedtaksperiodeOversiktForBehandling(vedtaksdata)
            is InnvilgelseEllerOpphørLæremidler -> lagVedtaksperiodeOversiktForBehandling(vedtaksdata)
            is InnvilgelseEllerOpphørBoutgifter -> lagVedtaksperiodeOversiktForBehandling(vedtaksdata)
            is InnvilgelseEllerOpphørDagligReise -> lagVedtaksperiodeOversiktForBehandling(vedtaksdata)
            else -> error("Vi støtter ikke å hente detaljertevedtaksperioder for denne stønadstypen enda")
        }
    }

    private fun oppsummerVedtaksperioderTilsynBarn(
        fagsakId: FagsakId,
    ): VedtaksperiodeOversiktForBehandling<DetaljertVedtaksperiodeTilsynBarn>? {
        val vedtakForSisteIverksatteBehandling =
            hentVedtaksdataForSisteIverksatteBehandling<InnvilgelseEllerOpphørTilsynBarn>(fagsakId)
                ?: return null

        return lagVedtaksperiodeOversiktForBehandling(vedtakForSisteIverksatteBehandling)
    }

    private fun lagVedtaksperiodeOversiktForBehandling(
        vedtakForSisteIverksatteBehandling: InnvilgelseEllerOpphørTilsynBarn,
    ): VedtaksperiodeOversiktForBehandling<DetaljertVedtaksperiodeTilsynBarn> =
        VedtaksperiodeOversiktForBehandling(
            detaljerteVedtaksperioder = vedtakForSisteIverksatteBehandling.finnDetaljerteVedtaksperioder(),
            vedtaksperioder = vedtakForSisteIverksatteBehandling.vedtaksperioder.map { Datoperiode(it.fom, it.tom) },
        )

    private fun oppsummerVedtaksperioderLæremidler(
        fagsakId: FagsakId,
    ): VedtaksperiodeOversiktForBehandling<DetaljertVedtaksperiodeLæremidler>? {
        val vedtakForSisteIverksatteBehandling =
            hentVedtaksdataForSisteIverksatteBehandling<InnvilgelseEllerOpphørLæremidler>(fagsakId)
                ?: return null

        return lagVedtaksperiodeOversiktForBehandling(vedtakForSisteIverksatteBehandling)
    }

    private fun lagVedtaksperiodeOversiktForBehandling(
        vedtakForSisteIverksatteBehandling: InnvilgelseEllerOpphørLæremidler,
    ): VedtaksperiodeOversiktForBehandling<DetaljertVedtaksperiodeLæremidler> =
        VedtaksperiodeOversiktForBehandling(
            detaljerteVedtaksperioder = vedtakForSisteIverksatteBehandling.finnDetaljerteVedtaksperioder(),
            vedtaksperioder = vedtakForSisteIverksatteBehandling.vedtaksperioder.map { Datoperiode(it.fom, it.tom) },
        )

    private fun oppsummerVedtaksperioderBoutgifter(
        fagsakId: FagsakId,
    ): VedtaksperiodeOversiktForBehandling<DetaljertVedtaksperiodeBoutgifter>? {
        val vedtakForSisteIverksatteBehandling =
            hentVedtaksdataForSisteIverksatteBehandling<InnvilgelseEllerOpphørBoutgifter>(fagsakId)
                ?: return null

        return lagVedtaksperiodeOversiktForBehandling(vedtakForSisteIverksatteBehandling)
    }

    private fun lagVedtaksperiodeOversiktForBehandling(
        vedtakForSisteIverksatteBehandling: InnvilgelseEllerOpphørBoutgifter,
    ): VedtaksperiodeOversiktForBehandling<DetaljertVedtaksperiodeBoutgifter> =
        VedtaksperiodeOversiktForBehandling(
            detaljerteVedtaksperioder = vedtakForSisteIverksatteBehandling.finnDetaljerteVedtaksperioder(),
            vedtaksperioder = vedtakForSisteIverksatteBehandling.vedtaksperioder.map { Datoperiode(it.fom, it.tom) },
        )

    private fun oppsummerVedtaksperioderDagligReiseTso(
        fagsakId: FagsakId,
    ): VedtaksperiodeOversiktForBehandling<DetaljertVedtaksperiodeDagligReiseTso>? {
        val vedtakForSisteIverksatteBehandling =
            hentVedtaksdataForSisteIverksatteBehandling<InnvilgelseEllerOpphørDagligReise>(fagsakId)
                ?: return null

        return lagVedtaksperiodeOversiktForBehandling(vedtakForSisteIverksatteBehandling)
    }

    private fun lagVedtaksperiodeOversiktForBehandling(
        vedtakForSisteIverksatteBehandling: InnvilgelseEllerOpphørDagligReise,
    ): VedtaksperiodeOversiktForBehandling<DetaljertVedtaksperiodeDagligReiseTso> =
        VedtaksperiodeOversiktForBehandling(
            detaljerteVedtaksperioder = vedtakForSisteIverksatteBehandling.finnDetaljerteVedtaksperioderTso(),
            vedtaksperioder = vedtakForSisteIverksatteBehandling.vedtaksperioder.map { Datoperiode(it.fom, it.tom) },
        )

    private fun oppsummerVedtaksperioderDagligReiseTsr(
        fagsakId: FagsakId,
    ): VedtaksperiodeOversiktForBehandling<DetaljertVedtaksperiodeDagligReiseTsr> =
        VedtaksperiodeOversiktForBehandling(emptyList(), emptyList())

    private inline fun <reified T : Vedtaksdata> hentVedtaksdataForSisteIverksatteBehandling(fagsakId: FagsakId): T? {
        val sisteIverksatteBehandling = behandlingService.finnSisteIverksatteBehandling(fagsakId) ?: return null
        val vedtak = vedtakService.hentVedtak<T>(sisteIverksatteBehandling.id) ?: return null
        return vedtak.data
    }
}
