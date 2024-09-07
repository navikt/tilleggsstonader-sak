package no.nav.tilleggsstonader.sak.brev

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Fil
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import org.springframework.data.annotation.Id
import java.time.LocalDateTime

data class Vedtaksbrev(
    @Id
    val behandlingId: BehandlingId,
    val saksbehandlerHtml: String,
    val saksbehandlersignatur: String,
    val besluttersignatur: String? = null,
    val beslutterPdf: Fil? = null,
    val saksbehandlerIdent: String,
    val beslutterIdent: String? = null,
    val opprettetTid: LocalDateTime = SporbarUtils.now(),
    val besluttetTid: LocalDateTime? = null,
)
