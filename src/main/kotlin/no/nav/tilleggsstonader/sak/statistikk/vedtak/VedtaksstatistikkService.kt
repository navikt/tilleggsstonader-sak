package no.nav.tilleggsstonader.sak.statistikk.vedtak


import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.UUID

@Service
class VedtaksstatistikkService(

) {
    fun lagreVedtaksstatistikk(behandlingId: UUID, fagsakId: UUID, hendelseTidspunkt: ZonedDateTime) {
        TODO()
    }
}
