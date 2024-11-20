package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class VedtaksresultatService(
    private val vedtakRepository: VedtakRepository,
) {

    fun hentVedtaksresultat(saksbehandling: Saksbehandling): TypeVedtak {
        return vedtakRepository.findByIdOrNull(saksbehandling.id)?.type
            ?: error("Finner ikke vedtaksresultat for behandling=$saksbehandling")
    }
}
