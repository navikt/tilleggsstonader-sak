package no.nav.tilleggsstonader.sak.kjøreliste

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("kjoreliste")
data class Kjøreliste(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column("journalpost_id")
    val journalpostId: String,
    val fagsakId: FagsakId,
    @Column("dato_mottatt")
    val datoMottatt: LocalDateTime,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    @Column("data")
    val data: InnsendtKjøreliste,
)
