package no.nav.tilleggsstonader.sak.brev.kjørelistebrev

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Fil
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("kjoreliste_behandling_brev")
data class KjørelisteBehandlingBrev(
    @Id
    val behandlingId: BehandlingId,
    val saksbehandlerHtml: String,
    val pdf: Fil,
    val saksbehandlerIdent: String,
    val opprettetTid: LocalDateTime = SporbarUtils.now(),
)
