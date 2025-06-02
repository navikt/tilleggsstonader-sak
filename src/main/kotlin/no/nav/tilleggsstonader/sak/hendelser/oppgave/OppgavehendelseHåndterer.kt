package no.nav.tilleggsstonader.sak.hendelser.oppgave

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.tilBehandlingstema
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import org.springframework.stereotype.Service

@Service
class OppgavehendelseHåndterer(
    private val oppgaveService: OppgaveService,
) {
    // For å kunne teste mottak av ConsumerRecord uten å måtte initialisere Kafka
    fun behandleOppgavehendelser(consumerRecords: List<OppgavehendelseRecord>) {
        consumerRecords
            .filter { it.erEndret() }
            .filter { it.erAktueltBehandlingstema() }
            .forEach {
                oppgaveService.håndterOppdatertOppgaveHendelse(
                    it.tilDomene(),
                )
            }
    }

    private fun OppgavehendelseRecord.erAktueltBehandlingstema(): Boolean =
        oppgave.kategorisering.behandlingstema in aktuelleBehandlingstema

    companion object {
        private val aktuelleBehandlingstema = Stønadstype.entries.map { it.tilBehandlingstema().value }
    }
}
