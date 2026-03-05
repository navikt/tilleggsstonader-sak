package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksdata
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class VedtakService(
    private val vedtakRepository: VedtakRepository,
) {
    fun hentVedtak(behandlingId: BehandlingId): Vedtak? = vedtakRepository.findByIdOrNull(behandlingId)

    @JvmName("hentTypetVedtak")
    final inline fun <reified T : Vedtaksdata> hentVedtak(behandlingId: BehandlingId): GeneriskVedtak<T>? =
        hentVedtak(behandlingId)?.withTypeOrThrow<T>()

    fun hentVedtaksperioder(behandlingId: BehandlingId): List<Vedtaksperiode> =
        hentVedtak(behandlingId)?.vedtaksperioderHvisFinnes() ?: emptyList()
}
