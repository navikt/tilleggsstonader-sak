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

    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "person_mottaker_")
    val personMottaker: BrevmottakerPerson? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "organisasjon_mottaker_")
    val organisasjonMottaker: BrevmottakerOrganisasjon? = null,

    val journalpostId: String? = null,
    val bestillingId: String? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    init {
        feilHvis(personMottaker == null && organisasjonMottaker == null) {
            "Ugyldig brevmottaker. personMottaker eller organisasjonMottaker må være satt"
        }

        feilHvis(personMottaker != null && organisasjonMottaker != null) {
            "Ugyldig brevmottaker. personMottaker og organisajsonMottaker kan ikke begge være satt"
        }
    }
}

enum class MottakerRolle {
    BRUKER,
    VERGE,
    FULLMAKT,
}

data class BrevmottakerPerson(val personIdent: String, val navn: String? = null, val mottakerRolle: MottakerRolle) {
    init {
        feilHvis(mottakerRolle != MottakerRolle.BRUKER && navn == null) {
            "Navn for brevmottaker må settes dersom mottaker ikke er bruker"
        }
    }
}

data class BrevmottakerOrganisasjon(
    val organisasjonsnummer: String,
    val navnHosOrganisasjon: String,
    val mottakerRolle: MottakerRolle,
)
