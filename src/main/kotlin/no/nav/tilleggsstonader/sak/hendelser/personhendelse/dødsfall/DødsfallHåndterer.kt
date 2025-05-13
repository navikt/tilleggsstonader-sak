package no.nav.tilleggsstonader.sak.hendelser.personhendelse.dødsfall

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.hendelser.Hendelse
import no.nav.tilleggsstonader.sak.hendelser.HendelseRepository
import no.nav.tilleggsstonader.sak.hendelser.TypeHendelse
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class DødsfallHåndterer(
    private val fagsakService: FagsakService,
    private val taskService: TaskService,
    private val personService: PersonService,
    private val behandlingService: BehandlingService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val hendelseRepository: HendelseRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun håndterDødsfall(dødsfallHendelse: DødsfallHendelse) {
        if (hendelseRepository.existsByTypeAndId(TypeHendelse.PERSONHENDELSE, dødsfallHendelse.hendelseId)) {
            logger.info("Dødsfallhendelse med id ${dødsfallHendelse.hendelseId} er allerede behandlet")
            return
        }

        val fagsakerMedAktiveVedtak = finnFagsakerMedLøpendeUtbetalingFraDødsdato(dødsfallHendelse)

        if (fagsakerMedAktiveVedtak.isNotEmpty()) {
            val opprettOppgaveTask = fagsakerMedAktiveVedtak.tilOpprettOppgaveTask()

            logger.info("Oppretter task for opprettelse av oppgave for håndtering av dødsfall som kjører ${opprettOppgaveTask.triggerTid}")
            val lagretTask = taskService.save(opprettOppgaveTask)

            hendelseRepository.insert(
                Hendelse(
                    type = TypeHendelse.PERSONHENDELSE,
                    id = dødsfallHendelse.hendelseId,
                    metadata = mapOf("taskId" to lagretTask.id.toString()),
                ),
            )
        }
    }

    private fun finnFagsakerMedLøpendeUtbetalingFraDødsdato(dødsfallHendelse: DødsfallHendelse) =
        fagsakService
            .finnFagsaker(dødsfallHendelse.personidenter.toSet())
            .filter { harAktivtVedtak(it, dødsfallHendelse.dødsdato) }

    private fun List<Fagsak>.tilOpprettOppgaveTask(): Task {
        val fagsak = first()
        logger.info("Oppretter oppgave for håndtering av dødsfall for person ${fagsak.id}")

        val personident =
            personService
                .hentFolkeregisterIdenter(fagsak.personIdenter.first().ident)
                .gjeldende()
                .ident

        // TODO - ved nye stønadstyper som ikke behandles av NAY bør vi lagre enhet på fagsak og opprette oppgave til alle enheter
        return OpprettOppgaveTask
            .opprettTask(
                personIdent = personident,
                stønadstype = fagsak.stønadstype,
                oppgave =
                    OpprettOppgave(
                        oppgavetype = Oppgavetype.VurderLivshendelse,
                        beskrivelse = "Person død, har løpende utbetalinger av ${joinToString(", ") { it.stønadstype.visningsnavn }}",
                    ),
            ).copy(triggerTid = LocalDateTime.now().plusWeeks(1))
    }

    private fun harAktivtVedtak(
        fagsak: Fagsak,
        dødsdato: LocalDate,
    ): Boolean {
        val behandling = behandlingService.finnSisteIverksatteBehandling(fagsak.id)
        if (behandling != null) {
            return vedtaksperiodeService
                .finnVedtaksperioderForBehandling(behandling.id, null)
                .any { it.inneholder(dødsdato) }
        }
        return false
    }
}
