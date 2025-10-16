package no.nav.tilleggsstonader.sak.hendelser.journalføring

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.sak.journalføring.brevkoder
import no.nav.tilleggsstonader.sak.journalføring.dokumentBrevkode
import org.springframework.stereotype.Component

@Component
class JournalpostMottattMetrikker {
    private val journalpostMottattCounters: Map<DokumentBrevkode, Counter> =
        DokumentBrevkode.entries.associateWith {
            Metrics.counter("journalpost.mottatt.brevkode", "type", it.name)
        }

    private val ukjentBrevkodeCounter: Counter = Metrics.counter("journalpost.mottatt.brevkode", "type", "ukjent")
    private val ingenBrevkodeCounter: Counter = Metrics.counter("journalpost.mottatt.brevkode", "type", "ingen")

    fun journalpostMottatt(journalpost: Journalpost) {
        val brevkode = journalpost.dokumentBrevkode()
        when {
            brevkode != null -> journalpostMottattCounters[brevkode]?.increment()
            journalpost.brevkoder().isEmpty() -> ingenBrevkodeCounter.increment()
            else -> ukjentBrevkodeCounter.increment()
        }
    }
}
