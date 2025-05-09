package no.nav.tilleggsstonader.sak.hendelser.personhendelse.dødsfall

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DødsfallHåndterer(
    private val fagsakService: FagsakService,
    private val taskService: TaskService,
    private val personService: PersonService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun håndterDødsfall(dødsfallHendelse: List<DødsfallHendelse>) {
        dødsfallHendelse
            .flatMap { fagsakService.finnFagsaker(it.personidenter.toSet()) }
            .filter { fagsakService.erLøpende(it.id) }
            .map {
                logger.info("Oppretter oppgave for håndtering av dødsfall for sak ${it.id}")
                it.tilOpprettOppgaveTask()
            }.let { taskService.saveAll(it) }
    }

    private fun Fagsak.tilOpprettOppgaveTask(): Task {
        val personident =
            personService
                .hentFolkeregisterIdenter(personIdenter.first().ident)
                .gjeldende()
                .ident

        return OpprettOppgaveTask.opprettTask(
            personIdent = personident,
            stønadstype = stønadstype,
            oppgave =
                OpprettOppgave(
                    oppgavetype = Oppgavetype.VurderLivshendelse,
                    beskrivelse = "Person død, har løpende utbetalinger av ${stønadstype.visningsnavn}",
                    tilordnetNavIdent = null,
                    // FIXME - gitt flere aktive stønader, må vi opprette oppgave til begge enheter? Enhetsnummer utledes fra stønadstype om null her
                    enhetsnummer = null,
                ),
        )
    }
}
