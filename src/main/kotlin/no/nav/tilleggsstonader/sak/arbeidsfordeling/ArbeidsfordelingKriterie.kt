package no.nav.tilleggsstonader.sak.arbeidsfordeling

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype

data class ArbeidsfordelingKriterie(
    val tema: String,
    val geografiskOmraade: String? = null,
    val diskresjonskode: String? = null,
    val skjermet: Boolean,
    val oppgavetype: Oppgavetype? = null,
)
