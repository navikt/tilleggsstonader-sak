package no.nav.tilleggsstonader.sak.brev

import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Brevmottaker(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY, prefix = "person_mottaker")
    val personMottaker: BrevmottakerPerson? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY, prefix = "organisasjon_mottaker")
    val organisasjonMottaker: BrevmottakerOrganisasjon? = null,

    val journalpostId: String? = null,
    val bestillingId: String? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    init {
        feilHvis(personMottaker == null && organisasjonMottaker == null){
            "Ugyldig brevmottaker. personMottaker eller organisasjonMottaker må være satt"
        }

        feilHvis(personMottaker != null && organisasjonMottaker != null){
            "Ugyldig brevmottaker. personMottaker og organisajsonMottaker kan ikke begge være satt"
        }
    }
}

enum class MottakerRolle {
    BRUKER,
    VERGE,
    FULLMAKT,
}

data class BrevmottakerPerson(val personIdent: String, val navn: String, val mottakerRolle: MottakerRolle)

data class BrevmottakerOrganisasjon(
    val organisasjonsnummer: String,
    val navnHosOrganisasjon: String,
    val mottakerRolle: MottakerRolle,
)
