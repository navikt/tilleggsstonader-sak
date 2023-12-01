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
        feilHvis(mottakerType == MottakerType.ORGANISASJON && navnHosOrganisasjon.isNullOrBlank()) {
            "Navn hos organisasjon er påkrevd"
        }

        feilHvis(mottakerRolle == MottakerRolle.BRUKER && mottakerType == MottakerType.ORGANISASJON) {
            "Ugyldig kombinasjon av mottakerType og mottakerRolle. Bruker/søker kan ikke være organisasjon."
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
