package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnBeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksdata
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerBeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtakLæremidlerRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class VedtakService(
    private val repository: VedtakRepository,
    private val stegService: StegService,
    private val tilsynBarnBeregnYtelseSteg: TilsynBarnBeregnYtelseSteg,
    private val læremidlerBeregnYtelseSteg: LæremidlerBeregnYtelseSteg,
) {
    fun hentVedtak(behandlingId: BehandlingId): Vedtak? = repository.findByIdOrNull(behandlingId)

    @JvmName("hentTypetVedtak")
    final inline fun <reified T : Vedtaksdata> hentVedtak(behandlingId: BehandlingId): GeneriskVedtak<T>? =
        hentVedtak(behandlingId)?.withTypeOrThrow<T>()

    fun håndterSteg(
        behandling: BehandlingId,
        data: VedtakTilsynBarnRequest,
    ) {
        stegService.håndterSteg(behandling, tilsynBarnBeregnYtelseSteg, data)
    }

    fun håndterSteg(
        behandling: BehandlingId,
        data: VedtakLæremidlerRequest,
    ) {
        stegService.håndterSteg(behandling, læremidlerBeregnYtelseSteg, data)
    }
}
