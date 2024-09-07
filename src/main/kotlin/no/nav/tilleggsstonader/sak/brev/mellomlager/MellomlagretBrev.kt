package no.nav.tilleggsstonader.sak.brev.mellomlager

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded

data class MellomlagretBrev(
    @Id
    val behandlingId: BehandlingId,
    val brevverdier: String,
    val brevmal: String,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
