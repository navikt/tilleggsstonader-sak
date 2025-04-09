package no.nav.tilleggsstonader.sak.opplysninger.oppgave.domain

data class FinnOppgaveresultatMedMetadata(
    val antallTreffTotalt: Long,
    val oppgaver: List<OppgaveMedMetadata>,
)
