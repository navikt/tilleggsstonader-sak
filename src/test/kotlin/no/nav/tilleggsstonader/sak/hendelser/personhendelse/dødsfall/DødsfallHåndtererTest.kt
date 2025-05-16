package no.nav.tilleggsstonader.sak.hendelser.personhendelse.dødsfall

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.StatusEnum
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.hendelser.Hendelse
import no.nav.tilleggsstonader.sak.hendelser.HendelseRepository
import no.nav.tilleggsstonader.sak.hendelser.TypeHendelse
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Properties
import java.util.UUID

class DødsfallHåndtererTest {
    private val fagsakService = mockk<FagsakService>()
    private val taskService = mockk<TaskService>(relaxed = true)
    private val personService = mockk<PersonService>()
    private val behandlingService = mockk<BehandlingService>()
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val hendelseRepository = mockk<HendelseRepository>()
    private val oppgaveService = mockk<OppgaveService>()
    private val dødsfallHåndterer =
        DødsfallHåndterer(
            fagsakService,
            taskService,
            behandlingService,
            vedtaksperiodeService,
            hendelseRepository,
            oppgaveService,
        )

    @Test
    fun `opprett oppgaver for dødsfall i saker med aktivt vedtak`() {
        val dødsfallHendelse =
            DødsfallHendelse(
                hendelseId = UUID.randomUUID().toString(),
                dødsdato = LocalDate.now(),
                personidenter = setOf("12345678901"),
                erAnnullering = false,
            )
        val fagsak = fagsak()
        val behandling = behandling()
        val vedtaksperiode =
            vedtaksperiode(
                fom = LocalDate.now().plusWeeks(1),
                tom = LocalDate.now().plusWeeks(2),
            )

        every { hendelseRepository.existsByTypeAndId(any(), any()) } returns false
        every { fagsakService.finnFagsaker(dødsfallHendelse.personidenter) } returns listOf(fagsak)
        every { behandlingService.finnSisteIverksatteBehandling(fagsak.id) } returns behandling
        every { vedtaksperiodeService.finnVedtaksperioderForBehandling(behandling.id, null) } returns listOf(vedtaksperiode)
        every { personService.hentFolkeregisterIdenter(any()).gjeldende().ident } returns "12345678901"
        every { hendelseRepository.insert(any()) } returnsArgument 0
        every { oppgaveService.lagBeskrivelseMelding(any(), any()) } returnsArgument 0

        dødsfallHåndterer.håndter(dødsfallHendelse)

        val taskSlot = slot<Task>()
        verify { taskService.save(capture(taskSlot)) }

        assertThat(taskSlot.captured.triggerTid).isCloseTo(LocalDateTime.now().plusWeeks(1), within(1, ChronoUnit.MINUTES))
    }

    @Test
    fun `ikke opprett oppgave for saker uten vedtak`() {
        val dødsfallHendelse =
            DødsfallHendelse(
                hendelseId = UUID.randomUUID().toString(),
                dødsdato = LocalDate.now(),
                personidenter = setOf("12345678901"),
                erAnnullering = false,
            )
        val fagsak = fagsak()
        val behandling = behandling()

        every { hendelseRepository.existsByTypeAndId(any(), any()) } returns false
        every { fagsakService.finnFagsaker(dødsfallHendelse.personidenter) } returns listOf(fagsak)
        every { behandlingService.finnSisteIverksatteBehandling(fagsak.id) } returns behandling
        every { vedtaksperiodeService.finnVedtaksperioderForBehandling(behandling.id, null) } returns emptyList()

        dødsfallHåndterer.håndter(dødsfallHendelse)

        verify(exactly = 0) { taskService.save(any()) }
    }

    @Test
    fun `ikke opprett oppgave for saker uten aktivt vedtak`() {
        val dødsfallHendelse =
            DødsfallHendelse(
                hendelseId = UUID.randomUUID().toString(),
                dødsdato = LocalDate.now(),
                personidenter = setOf("12345678901"),
                erAnnullering = false,
            )
        val fagsak = fagsak()
        val behandling = behandling()
        val vedtaksperiode =
            vedtaksperiode(
                fom = LocalDate.now().minusWeeks(2),
                tom = LocalDate.now().minusWeeks(1),
            )

        every { hendelseRepository.existsByTypeAndId(any(), any()) } returns false
        every { fagsakService.finnFagsaker(dødsfallHendelse.personidenter) } returns listOf(fagsak)
        every { behandlingService.finnSisteIverksatteBehandling(fagsak.id) } returns behandling
        every { vedtaksperiodeService.finnVedtaksperioderForBehandling(behandling.id, null) } returns listOf(vedtaksperiode)

        dødsfallHåndterer.håndter(dødsfallHendelse)

        verify(exactly = 0) { taskService.save(any()) }
    }

    @Test
    fun `håndter annullert dødshendelse hvor opprinnelig oppgave har blitt ferdigstilt`() {
        val annullertHendelseId = UUID.randomUUID().toString()
        val opprettetOppgaveId = 1111111L

        val opprettetTask =
            OpprettDødsfallOppgaveTask
                .opprettTask(
                    stønadstype = Stønadstype.LÆREMIDLER,
                    dødsfallHendelse =
                        DødsfallHendelse(
                            hendelseId = annullertHendelseId,
                            dødsdato = LocalDate.now(),
                            personidenter = setOf("12345678901"),
                            erAnnullering = false,
                        ),
                ).copy(
                    id = 9999L,
                    status = Status.FERDIG,
                    metadataWrapper =
                        PropertiesWrapper(
                            Properties().apply {
                                this["oppgaveId"] =
                                    opprettetOppgaveId
                            },
                        ),
                )

        val eksisterendeHendelse =
            Hendelse(
                type = TypeHendelse.PERSONHENDELSE,
                id = annullertHendelseId,
                metadata = mapOf("taskId" to listOf(opprettetTask.id)),
            )

        val oppgave =
            mockk<Oppgave>(relaxed = true) {
                every { status } returns StatusEnum.FERDIGSTILT
            }

        every { hendelseRepository.findByTypeAndId(TypeHendelse.PERSONHENDELSE, annullertHendelseId) } returns eksisterendeHendelse
        every { taskService.findById(opprettetTask.id) } returns opprettetTask
        every { oppgaveService.hentOppgave(opprettetOppgaveId) } returns oppgave

        dødsfallHåndterer.håndter(
            DødsfallHendelse(
                hendelseId = annullertHendelseId,
                dødsdato = LocalDate.now(),
                personidenter = setOf("1234567801"),
                erAnnullering = true,
            ),
        )

        verify(exactly = 1) { taskService.save(any()) }
    }

    @Test
    fun `håndter annullert dødshendelse når hendelse ikke finnes`() {
        val annullertHendelseId = UUID.randomUUID().toString()

        every { hendelseRepository.findByTypeAndId(TypeHendelse.PERSONHENDELSE, annullertHendelseId) } returns null

        dødsfallHåndterer.håndter(
            DødsfallHendelse(
                hendelseId = annullertHendelseId,
                dødsdato = LocalDate.now(),
                personidenter = setOf("1234567801"),
                erAnnullering = true,
            ),
        )

        verify(exactly = 0) { taskService.save(any()) }
        verify(exactly = 0) { oppgaveService.hentOppgave(any()) }
    }

    @Test
    fun `håndter annullert dødshendelse når opprettet task er ferdig`() {
        val annullertHendelseId = UUID.randomUUID().toString()

        val opprettetTask =
            OpprettOppgaveTask
                .opprettTask(
                    "12345678901",
                    Stønadstype.LÆREMIDLER,
                    OpprettOppgave(
                        oppgavetype = Oppgavetype.VurderLivshendelse,
                        beskrivelse = "dødsfall",
                    ),
                ).copy(
                    id = 9999L,
                    status = Status.FERDIG,
                    metadataWrapper =
                        PropertiesWrapper(
                            Properties().apply {
                                this["oppgaveId"] =
                                    12345
                            },
                        ),
                )

        val eksisterendeHendelse =
            Hendelse(
                type = TypeHendelse.PERSONHENDELSE,
                id = annullertHendelseId,
                metadata = mapOf("taskId" to listOf(opprettetTask.id)),
            )

        val oppgave =
            mockk<Oppgave>(relaxed = true) {
                every { status } returns StatusEnum.OPPRETTET
            }

        every { hendelseRepository.findByTypeAndId(TypeHendelse.PERSONHENDELSE, annullertHendelseId) } returns eksisterendeHendelse
        every { taskService.findById(9999L) } returns opprettetTask
        every { oppgaveService.hentOppgave(12345L) } returns oppgave
        every { oppgaveService.oppdaterOppgave(any()) } returns mockk()
        every { oppgaveService.lagBeskrivelseMelding(any(), any()) } returnsArgument 0

        dødsfallHåndterer.håndter(
            DødsfallHendelse(
                hendelseId = annullertHendelseId,
                dødsdato = LocalDate.now(),
                personidenter = setOf("1234567801"),
                erAnnullering = true,
            ),
        )

        verify { oppgaveService.oppdaterOppgave(any()) }
        verify(exactly = 0) { taskService.save(any()) }
    }

    @Test
    fun `håndter annullert dødshendelse når opprettet task ikke er kjørt`() {
        val annullertHendelseId = UUID.randomUUID().toString()

        val opprettetTask =
            OpprettOppgaveTask
                .opprettTask(
                    "12345678901",
                    Stønadstype.LÆREMIDLER,
                    OpprettOppgave(
                        oppgavetype = Oppgavetype.VurderLivshendelse,
                        beskrivelse = "dødsfall",
                    ),
                ).copy(id = 9999L, status = Status.UBEHANDLET)

        val eksisterendeHendelse =
            Hendelse(
                type = TypeHendelse.PERSONHENDELSE,
                id = annullertHendelseId,
                metadata = mapOf("taskId" to listOf(opprettetTask.id)),
            )

        every { hendelseRepository.findByTypeAndId(TypeHendelse.PERSONHENDELSE, annullertHendelseId) } returns eksisterendeHendelse
        every { taskService.findById(9999L) } returns opprettetTask

        dødsfallHåndterer.håndter(
            DødsfallHendelse(
                hendelseId = annullertHendelseId,
                dødsdato = LocalDate.now(),
                personidenter = setOf("1234567801"),
                erAnnullering = true,
            ),
        )

        verify(exactly = 0) { taskService.save(opprettetTask.copy(status = Status.AVVIKSHÅNDTERT)) }
        verify(exactly = 0) { oppgaveService.hentOppgave(any()) }
    }
}
