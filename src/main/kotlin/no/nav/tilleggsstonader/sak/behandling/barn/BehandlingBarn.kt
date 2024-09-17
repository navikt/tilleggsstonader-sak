package no.nav.tilleggsstonader.sak.behandling.barn

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded

/**
 * Usikker på om vi trenger [navn], kan denne hentes fra personopplysninger i stedet?
 */
data class BehandlingBarn(
    @Id
    val id: BarnId = BarnId.random(),
    val behandlingId: BehandlingId,
    val ident: String,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {

    fun erMatchendeBarn(annetBarn: BehandlingBarn): Boolean =
        this.ident == annetBarn.ident
}
