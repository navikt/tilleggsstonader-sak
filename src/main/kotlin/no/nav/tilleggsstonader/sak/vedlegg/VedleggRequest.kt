package no.nav.tilleggsstonader.sak.vedlegg

import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import java.util.UUID

data class VedleggRequest(
    val fagsakPersonId: UUID,
    val tema: List<Tema>? = emptyList(),
    val journalposttype: Journalposttype?,
    val journalstatus: Journalstatus?,
)
