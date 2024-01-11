package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain

import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.ÅrsakUnderkjent
import java.util.UUID

object TotrinnskontrollUtil {

    fun totrinnskontroll(
        status: TotrinnInternStatus,
        behandlingId: UUID = UUID.randomUUID(),
        saksbehandler: String = "saksbehandler",
        årsakerUnderkjent: List<ÅrsakUnderkjent> = emptyList(),
        begrunnelse: String? = null,
        beslutter: String? = null,
    ) = Totrinnskontroll(
        behandlingId = behandlingId,
        saksbehandler = saksbehandler,
        status = status,
        årsakerUnderkjent = Årsaker(årsakerUnderkjent),
        beslutter = beslutter,
        begrunnelse = begrunnelse,
    )
}
