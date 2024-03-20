package no.nav.tilleggsstonader.sak.journalføring.dto

import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost

data class JournalpostResponse(
    val journalpost: Journalpost,
    val personIdent: String,
    val navn: String,
    val harStrukturertSøknad: Boolean,
)
