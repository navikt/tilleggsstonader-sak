package no.nav.tilleggsstonader.sak.brev

import no.nav.tilleggsstonader.sak.infrastruktur.database.Fil
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.UUID

data class Vedtaksbrev(
    @Id
    val behandlingId: UUID,
    val saksbehandlerHtml: String,
    val saksbehandlersignatur: String,
    val besluttersignatur: String? = null,
    val beslutterPdf: Fil? = null,
    val saksbehandlerIdent: String,
    val beslutterIdent: String? = null,
    val opprettetTid: LocalDateTime? = null,
    val besluttetTid: LocalDateTime? = null,
)
