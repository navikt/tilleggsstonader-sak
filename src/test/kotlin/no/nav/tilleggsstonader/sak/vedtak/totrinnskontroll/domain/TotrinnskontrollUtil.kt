package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.ÅrsakUnderkjent

object TotrinnskontrollUtil {

    fun totrinnskontroll(
        status: TotrinnInternStatus,
        behandlingId: BehandlingId = BehandlingId.randomUUID(),
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
