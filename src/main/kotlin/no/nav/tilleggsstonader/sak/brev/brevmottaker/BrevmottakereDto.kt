package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import java.util.UUID

data class BrevmottakereDto(
    val personer: List<BrevmottakerPersonDto>,
    val organisasjoner: List<BrevmottakerOrganisasjonDto>,
)

data class BrevmottakerPersonDto(
    val id: UUID,
    val personIdent: String,
    val navn: String? = null,
    val mottakerRolle: MottakerRolle,
){
    init {
        feilHvis(mottakerRolle != MottakerRolle.BRUKER && navn == null) {
            "Navn for brevmottaker må settes dersom mottaker ikke er bruker"
        }
    }
}

data class BrevmottakerOrganisasjonDto(
    val id: UUID,
    val organisasjonsnummer: String,
    val navnHosOrganisasjon: String,
    val mottakerRolle: MottakerRolle,
)
