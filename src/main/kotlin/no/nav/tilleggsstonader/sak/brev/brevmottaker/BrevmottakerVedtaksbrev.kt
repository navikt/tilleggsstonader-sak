package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("brevmottaker")
data class BrevmottakerVedtaksbrev(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: BehandlingId,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val mottaker: Mottaker,

    val journalpostId: String? = null,
    val bestillingId: String? = null,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

data class Mottaker(
    val mottakerRolle: MottakerRolle,
    val mottakerType: MottakerType,
    val ident: String,
    val mottakerNavn: String? = null,
    val organisasjonsNavn: String? = null,
) {
    init {
        feilHvis(mottakerType == MottakerType.ORGANISASJON && mottakerNavn.isNullOrBlank()) {
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
