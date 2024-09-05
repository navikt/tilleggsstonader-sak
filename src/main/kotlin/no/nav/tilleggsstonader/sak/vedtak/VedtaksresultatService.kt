package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnVedtakRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class VedtaksresultatService(
    private val tilsynBarnRepository: TilsynBarnVedtakRepository,
) {

    fun hentVedtaksresultat(saksbehandling: Saksbehandling): TypeVedtak {
        return when (saksbehandling.stønadstype) {
            Stønadstype.BARNETILSYN -> tilsynBarnRepository.findByIdOrNull(saksbehandling.id)?.type
            else -> error("Kan ikke hente vedtaksresultat for stønadstype ${saksbehandling.stønadstype}.")
        } ?: error("Finner ikke vedtaksresultat for behandling=$saksbehandling")
    }
}
