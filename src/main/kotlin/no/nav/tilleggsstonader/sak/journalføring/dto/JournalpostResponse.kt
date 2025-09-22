package no.nav.tilleggsstonader.sak.journalføring.dto

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost

data class JournalpostResponse(
    val journalpost: Journalpost,
    val personIdent: String,
    val navn: String,
    val harStrukturertSøknad: Boolean,
    val valgbareStønadstyper: List<Stønadstype>,
)
