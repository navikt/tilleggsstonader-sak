package no.nav.tilleggsstonader.sak.hendelser.personhendelse.dødsfall

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.hendelser.Hendelse
import no.nav.tilleggsstonader.sak.hendelser.HendelseRepository
import no.nav.tilleggsstonader.sak.hendelser.TypeHendelse
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.util.erÅpen
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
    private val behandlingService: BehandlingService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val hendelseRepository: HendelseRepository,
    private val oppgaveService: OppgaveService,
) {
    companion object {
        private const val ANNULLERT_DØDSFALL_BESKRIVELSE = "\n\nDenne oppgaven har blitt annullert"
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun håndter(dødsfallHendelse: DødsfallHendelse) {
        if (hendelseRepository.existsByTypeAndId(TypeHendelse.PERSONHENDELSE, dødsfallHendelse.hendelseId)) {
            logger.info("Dødsfallhendelse med id ${dødsfallHendelse.hendelseId} er allerede behandlet")
        } else {
            håndterDødsfall(dødsfallHendelse)
        }
    }

    private fun håndterDødsfall(dødsfallHendelse: DødsfallHendelse) {
        val fagsakerMedAktiveVedtak = finnFagsakerMedLøpendeUtbetalingFraDødsdato(dødsfallHendelse)

        if (fagsakerMedAktiveVedtak.isNotEmpty()) {
            val opprettOppgaveTask = fagsakerMedAktiveVedtak.opprettTask(dødsfallHendelse)

            logger.info("Oppretter task for opprettelse av oppgave for håndtering av dødsfall som kjører ${opprettOppgaveTask.triggerTid}")
            val lagretTask = taskService.save(opprettOppgaveTask)

            hendelseRepository.insert(
                Hendelse(
                    type = TypeHendelse.PERSONHENDELSE,
                    id = dødsfallHendelse.hendelseId,
                    metadata = mapOf("taskId" to listOf(lagretTask.id.toString())),
                ),
            )
        }
    }

    private fun finnFagsakerMedLøpendeUtbetalingFraDødsdato(dødsfallHendelse: DødsfallHendelse) =
        fagsakService
            .finnFagsaker(dødsfallHendelse.personidenter.toSet())
            .filter { harAktivtVedtak(it, dødsfallHendelse.dødsdato) }

    private fun List<Fagsak>.opprettTask(dødsfallHendelse: DødsfallHendelse): Task {
        val fagsak = first()
        logger.info("Oppretter oppgave for håndtering av dødsfall for person ${fagsak.id}")

        // TODO - ved nye stønadstyper som ikke behandles av NAY bør vi lagre enhet på fagsak og opprette oppgave til alle enheter
        return OpprettDødsfallOppgaveTask
            .opprettTask(
                stønadstype = fagsak.stønadstype,
                dødsfallHendelse = dødsfallHendelse,
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
                .any { dødsdato <= it.tom }
        }
        return false
    }

    fun håndterAnnullertDødsfall(annullertHendelseId: String) {
        val eksisterendeHendelse = hendelseRepository.findByTypeAndId(TypeHendelse.PERSONHENDELSE, annullertHendelseId)
        if (eksisterendeHendelse != null) {
            håndterAnnullertDødsfallhendelse(eksisterendeHendelse)
        } else {
            logger.debug("Annullert dødsfallhendelse $annullertHendelseId")
        }
    }

    private fun håndterAnnullertDødsfallhendelse(hendelse: Hendelse) {
        val taskId = objectMapper.readTree(hendelse.metadata!!.json)["taskId"].first().asLong()
        val opprinneligTask = taskService.findById(taskId)
        if (opprinneligTask.status != Status.FERDIG) {
            logger.info(
                "Mottatt annullering av dødshendelse ${hendelse.id}, opprinnelig task er ikke kjørt, lar den kjøre ferdig",
            )
        } else {
            val oppgave = oppgaveService.hentOppgave(opprinneligTask.metadata["oppgaveId"]!!.toString().toLong())
            if (oppgave.erÅpen()) {
                logger.info(
                    "Oppgave er allerede opprettet og åpen for dødshendelse ${hendelse.id}, oppdaterer oppgave",
                )
                oppgaveService.oppdaterOppgave(
                    oppgave.copy(
                        beskrivelse = oppgaveService.lagBeskrivelseMelding(ANNULLERT_DØDSFALL_BESKRIVELSE, oppgave.beskrivelse),
                    ),
                )
            } else {
                logger.info("Tidligere oppgave for dødsfall er ferdigstilt, oppretter ny oppgave for annullering av dødsfall")
                val tidligereTaskData = objectMapper.readValue<OpprettDødsfallOppgaveTask.DødsfallOppgaveTaskData>(opprinneligTask.payload)
                taskService.save(
                    OpprettDødsfallOppgaveTask.opprettTask(
                        tidligereTaskData
                            .copy(
                                erAnnullering = true,
                                beskrivelse =
                                    "Tidligere oppgave for håndtering av dødsfall-hendelse har blitt ferdigstilt. " +
                                        "Denne hendelsen er nå blitt annullert.",
                            ),
                    ),
                )
            }
        }
    }
}
