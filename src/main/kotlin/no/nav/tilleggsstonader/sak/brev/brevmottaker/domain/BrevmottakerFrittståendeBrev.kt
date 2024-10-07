package no.nav.tilleggsstonader.sak.brev.brevmottaker.domain

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("brevmottaker_frittstaende_brev")
data class BrevmottakerFrittståendeBrev(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakId: FagsakId,
    val brevId: UUID? = null,
    val journalpostId: String? = null,
    val bestillingId: String? = null,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val mottaker: Mottaker,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
