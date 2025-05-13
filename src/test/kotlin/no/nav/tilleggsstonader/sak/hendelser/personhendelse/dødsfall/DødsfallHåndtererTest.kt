package no.nav.tilleggsstonader.sak.hendelser.personhendelse.dødsfall

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.hendelser.HendelseRepository
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeService
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DødsfallHåndtererTest {
    private val fagsakService = mockk<FagsakService>()
    private val taskService = mockk<TaskService>(relaxed = true)
    private val personService = mockk<PersonService>()
    private val behandlingService = mockk<BehandlingService>()
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val hendelseRepository = mockk<HendelseRepository>()
    private val dødsfallHåndterer =
        DødsfallHåndterer(
            fagsakService,
            taskService,
            personService,
            behandlingService,
            vedtaksperiodeService,
            hendelseRepository,
        )

    @Test
    fun `opprett oppgaver for dødsfall i saker med aktivt vedtak`() {
        val dødsfallHendelse =
            DødsfallHendelse(
                hendelseId = UUID.randomUUID().toString(),
                dødsdato = LocalDate.now(),
                personidenter = setOf("12345678901"),
            )
        val fagsak = fagsak()
        val behandling = behandling()
        val vedtaksperiode =
            vedtaksperiode(
                fom = LocalDate.now().minusWeeks(1),
                tom = LocalDate.now().plusWeeks(1),
            )

        every { hendelseRepository.existsByTypeAndId(any(), any()) } returns false
        every { fagsakService.finnFagsaker(dødsfallHendelse.personidenter) } returns listOf(fagsak)
        every { behandlingService.finnSisteIverksatteBehandling(fagsak.id) } returns behandling
        every { vedtaksperiodeService.finnVedtaksperioderForBehandling(behandling.id, null) } returns listOf(vedtaksperiode)
        every { personService.hentFolkeregisterIdenter(any()).gjeldende().ident } returns "12345678901"
        every { hendelseRepository.insert(any()) } returnsArgument 0

        dødsfallHåndterer.håndterDødsfall(dødsfallHendelse)

        verify { taskService.save(any()) }
    }

    @Test
    fun `ikke opprett oppgave for saker uten vedtak`() {
        val dødsfallHendelse =
            DødsfallHendelse(
                hendelseId = UUID.randomUUID().toString(),
                dødsdato = LocalDate.now(),
                personidenter = setOf("12345678901"),
            )
        val fagsak = fagsak()
        val behandling = behandling()

        every { hendelseRepository.existsByTypeAndId(any(), any()) } returns false
        every { fagsakService.finnFagsaker(dødsfallHendelse.personidenter) } returns listOf(fagsak)
        every { behandlingService.finnSisteIverksatteBehandling(fagsak.id) } returns behandling
        every { vedtaksperiodeService.finnVedtaksperioderForBehandling(behandling.id, null) } returns emptyList()

        dødsfallHåndterer.håndterDødsfall(dødsfallHendelse)

        verify(exactly = 0) { taskService.save(any()) }
    }

    @Test
    fun `ikke opprett oppgave for saker uten aktivt vedtak`() {
        val dødsfallHendelse =
            DødsfallHendelse(
                hendelseId = UUID.randomUUID().toString(),
                dødsdato = LocalDate.now(),
                personidenter = setOf("12345678901"),
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

        dødsfallHåndterer.håndterDødsfall(dødsfallHendelse)

        verify(exactly = 0) { taskService.save(any()) }
    }
}
