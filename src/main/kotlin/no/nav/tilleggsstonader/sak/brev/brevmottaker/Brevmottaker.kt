package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Brevmottaker(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val mottakerRolle: MottakerRolle,
    val mottakerType: MottakerType,
    val ident: String,
    val navnHosOrganisasjon: String? = null,

    val journalpostId: String? = null,
    val bestillingId: String? = null,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    init {
        feilHvis(mottakerType == MottakerType.ORGANISASJON && navnHosOrganisasjon == null) {
            "Navn hos organisasjon er påkrevd"
        }
    }
}

enum class MottakerRolle {
    BRUKER,
    VERGE,
    FULLMAKT,
}

enum class MottakerType {
    PERSON,
    ORGANISASJON,
}
