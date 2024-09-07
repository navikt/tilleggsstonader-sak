package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.ÅrsakUnderkjent
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Totrinnskontroll(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: BehandlingId,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    val saksbehandler: String,
    val status: TotrinnInternStatus,
    @Column("årsak")
    val årsakerUnderkjent: Årsaker? = null,
    val begrunnelse: String? = null,
    val beslutter: String? = null,
)

data class Årsaker(
    val årsaker: List<ÅrsakUnderkjent>,
)

enum class TotrinnInternStatus {
    UNDERKJENT,
    GODKJENT,
    ANGRET,
    KAN_FATTE_VEDTAK,
}
