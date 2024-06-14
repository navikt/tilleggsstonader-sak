package no.nav.tilleggsstonader.sak.vedlegg

import no.nav.tilleggsstonader.kontrakter.felles.Arkivtema
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus

data class VedleggRequest(
    val tema: List<Arkivtema>? = emptyList(),
    val journalposttype: Journalposttype?,
    val journalstatus: Journalstatus?,
)
