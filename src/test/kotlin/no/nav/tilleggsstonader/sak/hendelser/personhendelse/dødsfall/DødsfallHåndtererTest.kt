package no.nav.tilleggsstonader.sak.hendelser.personhendelse.dødsfall

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeService
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DødsfallHåndtererTest {
    private val fagsakService = mockk<FagsakService>()
    private val taskService = mockk<TaskService>(relaxed = true)
    private val personService = mockk<PersonService>()
    private val behandlingService = mockk<BehandlingService>()
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val dødsfallHåndterer =
        DødsfallHåndterer(
            fagsakService,
            taskService,
            personService,
            behandlingService,
            vedtaksperiodeService,
        )

    @Test
    fun `opprett oppgaver for dødsfall i saker med aktivt vedtak`() {
        val dødsfallHendelse =
            DødsfallHendelse(
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

        every { fagsakService.finnFagsaker(dødsfallHendelse.personidenter) } returns listOf(fagsak)
        every { behandlingService.finnSisteIverksatteBehandling(fagsak.id) } returns behandling
        every { vedtaksperiodeService.finnVedtaksperioderForBehandling(behandling.id, null) } returns listOf(vedtaksperiode)
        every { personService.hentFolkeregisterIdenter(any()).gjeldende().ident } returns "12345678901"

        dødsfallHåndterer.håndterDødsfall(listOf(dødsfallHendelse))

        verify { taskService.saveAll(any()) }
    }

    @Test
    fun `ikke opprett oppgave for saker uten vedtak`() {
        val dødsfallHendelse =
            DødsfallHendelse(
                dødsdato = LocalDate.now(),
                personidenter = setOf("12345678901"),
            )
        val fagsak = fagsak()
        val behandling = behandling()

        every { fagsakService.finnFagsaker(dødsfallHendelse.personidenter) } returns listOf(fagsak)
        every { behandlingService.finnSisteIverksatteBehandling(fagsak.id) } returns behandling
        every { vedtaksperiodeService.finnVedtaksperioderForBehandling(behandling.id, null) } returns emptyList()

        dødsfallHåndterer.håndterDødsfall(listOf(dødsfallHendelse))

        verify(exactly = 0) { taskService.saveAll(any()) }
    }

    @Test
    fun `ikke opprett oppgave for saker uten aktivt vedtak`() {
        val dødsfallHendelse =
            DødsfallHendelse(
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

        every { fagsakService.finnFagsaker(dødsfallHendelse.personidenter) } returns listOf(fagsak)
        every { behandlingService.finnSisteIverksatteBehandling(fagsak.id) } returns behandling
        every { vedtaksperiodeService.finnVedtaksperioderForBehandling(behandling.id, null) } returns listOf(vedtaksperiode)

        dødsfallHåndterer.håndterDødsfall(listOf(dødsfallHendelse))

        verify(exactly = 0) { taskService.saveAll(any()) }
    }

    @Test
    fun `håndter at service kalles med tom liste`() {
        dødsfallHåndterer.håndterDødsfall(emptyList())

        verify(exactly = 0) { taskService.saveAll(any()) }
    }
}
