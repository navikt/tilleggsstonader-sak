package no.nav.tilleggsstonader.sak.hendelser.oppgave

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.tilBehandlingstema
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service

@Service
class OppgavehendelseHåndterer(
    private val oppgaveService: OppgaveService,
) {
    companion object {
        private val aktuelleBehandlingstema = Stønadstype.entries.map { it.tilBehandlingstema().value }
    }

    // For å kunne teste mottak av ConsumerRecord uten å måtte initialisere Kafka
    fun behandleOppgavehendelser(consumerRecords: List<ConsumerRecord<String, OppgaveRecord>>) {
        consumerRecords
            .filter { it.value().erEndret() }
            .filter { it.value().erAktueltBehandlingstema() }
            .forEach {
                oppgaveService.håndterOppdatertOppgaveHendelse(
                    it.value().tilDomene(),
                )
            }
    }

    private fun OppgaveRecord.erAktueltBehandlingstema(): Boolean = oppgave.kategorisering.behandlingstema in aktuelleBehandlingstema
}
