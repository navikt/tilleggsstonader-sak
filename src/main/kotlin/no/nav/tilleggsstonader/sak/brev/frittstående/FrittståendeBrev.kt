package no.nav.tilleggsstonader.sak.brev.frittstående

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Fil
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("frittstaende_brev")
data class FrittståendeBrev(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakId: FagsakId,
    val pdf: Fil,
    val tittel: String,
    val saksbehandlerIdent: String = SikkerhetContext.hentSaksbehandler(),
    val opprettetTid: LocalDateTime = SporbarUtils.now(),
)
