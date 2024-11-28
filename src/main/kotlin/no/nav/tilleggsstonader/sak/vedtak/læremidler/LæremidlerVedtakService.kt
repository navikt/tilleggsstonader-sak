package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtakLæremidlerRequest
import org.springframework.stereotype.Service

@Service
class LæremidlerVedtakService(
    repository: VedtakRepository,
    stegService: StegService,
    beregnYtelseSteg: LæremidlerBeregnYtelseSteg,
) : VedtakService<VedtakLæremidlerRequest>(stegService, beregnYtelseSteg, repository)
