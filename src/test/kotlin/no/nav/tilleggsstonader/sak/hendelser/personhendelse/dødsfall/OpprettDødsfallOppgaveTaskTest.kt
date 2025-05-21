package no.nav.tilleggsstonader.sak.hendelser.personhendelse.dødsfall

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.FolkeregisteridentifikatorStatus
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlSøker
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class OpprettDødsfallOppgaveTaskTest {
    private val personService = mockk<PersonService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val opprettDødsfallOppgaveTask = OpprettDødsfallOppgaveTask(personService, oppgaveService)

    @Test
    fun `oppretter oppgave når personen fortsatt er død`() {
        val personident = "12345678901"
        val dødsfallHendelse = DødsfallHendelse(UUID.randomUUID().toString(), LocalDate.now(), setOf(personident))
        val task = OpprettDødsfallOppgaveTask.opprettTask(Stønadstype.LÆREMIDLER, dødsfallHendelse)
        val person =
            mockk<PdlSøker> {
                every { dødsfall } returns listOf(mockk { every { dødsdato } returns LocalDate.now() })
                every { folkeregisteridentifikator } returns
                    listOf(
                        mockk {
                            every { status } returns FolkeregisteridentifikatorStatus.I_BRUK
                            every { ident } returns personident
                        },
                    )
            }

        every { personService.hentPersonUtenBarn(personident) } returns mockk { every { søker } returns person }
        every { oppgaveService.opprettOppgave(any(), any(), any(), any()) } returns 1L

        opprettDødsfallOppgaveTask.doTask(task)

        verify { oppgaveService.opprettOppgave(personident, Stønadstype.LÆREMIDLER, null, any()) }
    }

    @Test
    fun `oppretter ikke oppgave når person ikke er død`() {
        val personident = "12345678901"
        val dødsfallHendelse = DødsfallHendelse(UUID.randomUUID().toString(), LocalDate.now(), setOf(personident))
        val task = OpprettDødsfallOppgaveTask.opprettTask(Stønadstype.LÆREMIDLER, dødsfallHendelse)
        val person =
            mockk<PdlSøker> {
                every { dødsfall } returns emptyList()
                every { folkeregisteridentifikator } returns
                    listOf(
                        mockk {
                            every { status } returns FolkeregisteridentifikatorStatus.I_BRUK
                            every { ident } returns personident
                        },
                    )
            }

        every { personService.hentPersonUtenBarn(personident) } returns mockk { every { søker } returns person }

        opprettDødsfallOppgaveTask.doTask(task)

        verify(exactly = 0) { oppgaveService.opprettOppgave(any(), any(), any(), any()) }
    }
}
