package no.nav.tilleggsstonader.sak.behandling.barn

import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

/**
 * Usikker på om vi trenger [navn], kan denne hentes fra personopplysninger i stedet?
 */
data class BehandlingBarn(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val personIdent: String,
    val navn: String,
    @Column("soknad_barn_id")
    val søknadBarnId: UUID? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {

    fun erMatchendeBarn(annetBarn: BehandlingBarn): Boolean =
        (this.personIdent == annetBarn.personIdent)
    // || (this.søknadBarnId != null && this.søknadBarnId == annetBarn.søknadBarnId)
}
