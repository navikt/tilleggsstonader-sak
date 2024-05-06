package no.nav.tilleggsstonader.sak.statistikk.vedtak

import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class VedtaksstatistikkService() {
    fun lagreVedtaksstatistikk(behandlingId: UUID, fagsakId: UUID, hendelseTidspunkt: LocalDateTime) {
        TODO()
    }
}
