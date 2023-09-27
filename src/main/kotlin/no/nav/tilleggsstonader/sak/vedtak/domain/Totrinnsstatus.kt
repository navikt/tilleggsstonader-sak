package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.vedtak.dto.Årsak
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Totrinnsstatus(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    val saksbehandler: String,
    val status: String,
    @Column("årsak")
    val årsakerUnderkjent: Årsaker? = null,
    val begrunnelse: String? = null,
    val beslutter: String? = null,

)
data class Årsaker(
    val årsaker: List<Årsak>,
)
