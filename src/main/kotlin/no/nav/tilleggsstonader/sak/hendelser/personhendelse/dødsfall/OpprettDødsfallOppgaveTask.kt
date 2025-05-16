package no.nav.tilleggsstonader.sak.hendelser.personhendelse.dødsfall

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.FolkeregisteridentifikatorStatus
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlSøker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettDødsfallOppgaveTask.TYPE,
    beskrivelse = "Oppretter oppgave for dødsfall for gitt stønadstype",
)
class OpprettDødsfallOppgaveTask(
    private val personService: PersonService,
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val data = objectMapper.readValue<DødsfallOppgaveTaskData>(task.payload)
        val dødsfallHendelse = data.dødsfallHendelse

        val person = personService.hentPersonUtenBarn(dødsfallHendelse.personidenter.first())

        if (dødsfallHendelse.erAnnullering || personErFortsattDød(person.søker)) {
            val folkeregisterIdent =
                person.søker.folkeregisteridentifikator
                    .first { it.status == FolkeregisteridentifikatorStatus.I_BRUK }
                    .ident

            val oppgaveId =
                oppgaveService.opprettOppgave(
                    personIdent = folkeregisterIdent,
                    stønadstype = data.stønadstype,
                    behandlingId = null,
                    oppgave =
                        OpprettOppgave(
                            oppgavetype = Oppgavetype.VurderLivshendelse,
                            beskrivelse = data.beskrivelse ?: "Person død, vurder aktive stønader",
                        ),
                )
            task.metadata["oppgaveId"] = oppgaveId.toString()
        } else {
            logger.info("Oppgave for dødsfall med hendelseId=${dødsfallHendelse.hendelseId} er ikke opprettet fordi personen ikke er død")
        }
    }

    private fun personErFortsattDød(person: PdlSøker): Boolean =
        person
            .dødsfall
            .any { it.dødsdato != null }

    data class DødsfallOppgaveTaskData(
        val dødsfallHendelse: DødsfallHendelse,
        val stønadstype: Stønadstype,
        val beskrivelse: String? = null,
    )

    companion object {
        const val TYPE = "opprettDødsfallOppgave"

        fun opprettTask(dødsfallOppgaveTaskData: DødsfallOppgaveTaskData) =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(dødsfallOppgaveTaskData),
            )

        fun opprettTask(
            stønadstype: Stønadstype,
            dødsfallHendelse: DødsfallHendelse,
        ): Task =
            Task(
                type = TYPE,
                payload =
                    objectMapper.writeValueAsString(
                        DødsfallOppgaveTaskData(
                            dødsfallHendelse = dødsfallHendelse,
                            stønadstype = stønadstype,
                        ),
                    ),
            )
    }
}
